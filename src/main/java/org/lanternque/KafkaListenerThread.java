package org.lanternque;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaListenerThread implements Runnable {

    String bootstrapServers = "127.0.0.1:9092";
    String groupId = "my-fourth-application";
    String topic = "quickstart-events";

    public KafkaListenerThread(String brokers, String topic) {
        bootstrapServers = brokers;
        this.topic = topic;
    }

    @Override
    public void run() {
        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // create consumer
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            // subscribe consumer to our topic(s)
            consumer.subscribe(Collections.singletonList(topic));

            for (;;) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                if (Thread.currentThread().isInterrupted())
                    return;
                for (ConsumerRecord<String, String> record : records) {
                    String msg = record.value();
                    UIThread.addMessage(msg);
                }
            }
        } catch (InterruptException ie) {
            System.out.println("Kafka listening thread interrupted.");
        }
    }
}
