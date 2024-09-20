package org.lanternque;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class UIThread implements Runnable {

    static LinkedBlockingQueue<String> consumerQueue = new LinkedBlockingQueue<>();
    private static TextBox kafkaBrokers;
    private static TextBox topic;
    private static TextBox numLines;
    private static TextBox output;

    static ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();
    static ExecutorService msgDisplayExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void run() {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        Screen screen = null;
        try {
            screen = terminalFactory.createScreen();
            screen.startScreen();
            int width = screen.getTerminalSize().getColumns();
            int height = screen.getTerminalSize().getRows();

            final WindowBasedTextGUI textGUI =
                    new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                            new EmptySpace(new TextColor.RGB(20, 20, 20)));
            textGUI.setTheme(getTheme());
            final Window window = new BasicWindow();

            List<Window.Hint> hints = new ArrayList<>();
            hints.add(Window.Hint.FULL_SCREEN);
            window.setHints(hints);

            Theme theme = getTheme();
            window.setTheme(theme);

            Panel contentPanel = new Panel(new GridLayout(2));
            contentPanel.setTheme(theme);

            // servers & ports
            contentPanel.addComponent(new Label("Kafka Brokers:"));
            TerminalSize editFieldSize = new TerminalSize(width/2-2, 1);
            kafkaBrokers = new TextBox(editFieldSize);
            kafkaBrokers.setText("127.0.0.1:9092");
            contentPanel.addComponent(kafkaBrokers);

            // topic
            contentPanel.addComponent(new Label("Topic"));
            topic = new TextBox(editFieldSize);
            topic.setText("quickstart-events");
            contentPanel.addComponent(topic);
            
            // max # messages to keep before discarding
            contentPanel.addComponent(new Label("Buffer Size"));
            numLines = new TextBox(editFieldSize);
            numLines.setText("20");
            contentPanel.addComponent(numLines);
            
            // add spacers
            contentPanel.addComponent(new EmptySpace());
            contentPanel.addComponent(new EmptySpace());

            // display area
            int totalRows = screen.getTerminalSize().getRows();
            int totalCols = screen.getTerminalSize().getColumns();
            output = new TextBox(new TerminalSize(width, height-8));
            output.setReadOnly(true);
            contentPanel.addComponent(output, GridLayout.createLayoutData(
                    GridLayout.Alignment.BEGINNING, // Horizontal alignment in the grid cell if the cell is larger than the component's preferred size
                    GridLayout.Alignment.BEGINNING, // Vertical alignment in the grid cell if the cell is larger than the component's preferred size
                    true,       // Give the component extra horizontal space if available
                    true,        // Give the component extra vertical space if available
                    2,                  // Horizontal span
                    1));

            // start listening
            contentPanel.addComponent(new Button("Start", new Runnable() {
                @Override
                public void run() {
                    // creating a new listener thread kills off the old one (if any)
                    String brokers = kafkaBrokers.getText();
                    String topicName  = topic.getText();
                    KafkaListenerThread kafkaListenerThread = new KafkaListenerThread(brokers, topicName);
                    listenerExecutor.shutdownNow();
                    try {
                        listenerExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ie) {
                        //
                    }
                    listenerExecutor = Executors.newSingleThreadExecutor();
                    listenerExecutor.execute(kafkaListenerThread);
                }
            }));


            // exit button
            contentPanel.addComponent(new Button("Exit", new Runnable() {
                @Override
                public void run() {
                    listenerExecutor.shutdownNow();
                    try {
                        listenerExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        // ok to ignore because we're terminating
                    }
                    System.exit(0);
                }
            }));

            window.setComponent(contentPanel);
            textGUI.addWindowAndWait(window);

        } catch (Throwable t) {
            System.err.println("Error: " + t.getMessage());
        } finally {
            if(screen != null) {
                try {
                    screen.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void addMessage(String msg) {
        MessageReceiver messageReceiver = new MessageReceiver(msg);
        msgDisplayExecutor.submit(messageReceiver);
    }

    public static Theme getTheme() {
        TextColor text = TextColor.ANSI.WHITE;
        TextColor background = TextColor.ANSI.BLACK;
        TextColor textColorCurField = new TextColor.RGB(120, 120, 255);

        Theme myTheme = SimpleTheme.makeTheme(true, text,
                background, textColorCurField, new TextColor.RGB(30,30,30),
                new TextColor.RGB(190,190,190), new TextColor.RGB(10,10,10),TextColor.ANSI.BLACK);
        return myTheme;
    }

    public static class MessageReceiver implements Runnable {
        String msg;

        public MessageReceiver(String m) {
            msg = m;
        }

        @Override
        public void run() {
            String lineLimit = numLines.getText();
            int maxLines = Integer.parseInt(lineLimit);
            int curLines = output.getLineCount();
            if (curLines > maxLines)
                output.removeLine(0);
            output.addLine(msg);
        }
    }

}
