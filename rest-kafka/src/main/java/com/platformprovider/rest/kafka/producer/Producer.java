package com.platformprovider.rest.kafka.producer;

/**
 * Created by arincon on 5/06/17.
 */

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Producer extends Thread {
    private final KafkaProducer<Integer, String> producer;
    private final String topic;

    public Producer(String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        props.put("client.id", "DemoProducer");
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("security.protocol","SSL");
        props.put("ssl.truststore.location","/kafka-server-trustore.jks");
        props.put("ssl.truststore.password","c4_trust_d3f4ult_k3yst0r3");
        props.put("ssl.keystore.location","/client_kafka.jks");
        props.put("ssl.keystore.password","br0k3r_0_c0nflu3nt_k4fk4_s3c");
        props.put("ssl.key.password","br0k3r_0_c0nflu3nt_k4fk4_s3c");
        producer = new KafkaProducer<>(props);
        this.topic = topic;
    }


    public void send(String body) throws ExecutionException, InterruptedException {
        producer.send(new ProducerRecord(topic,
                body)).get();
    }
}


