package org.lanternque;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class KafkaProducerThread implements Runnable {

    static LinkedBlockingQueue<String> producerQueue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        Properties kafkaProps = new Properties();

        try {
            kafkaProps.load(new FileReader("producer.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (KafkaProducer<String,String> producer = new KafkaProducer<>(kafkaProps)) {
            ProducerRecord<String, String> record = new ProducerRecord<>("my-topic", "my-key", "test");
            producer.send(record);
            producer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
