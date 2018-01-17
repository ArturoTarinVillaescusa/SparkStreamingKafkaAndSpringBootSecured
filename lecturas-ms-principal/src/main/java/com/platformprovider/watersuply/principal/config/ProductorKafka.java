package com.platformprovider.watersupply.principal.config;

/**
 * Created by atarin on 5/06/17.
 */

import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProductorKafka extends Thread {
    private static KafkaProducer<Integer, String> producer;
    private final String topic;

    public ProductorKafka(String topic) {
        Properties props = new Properties();

		props.put("bootstrap.servers", KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        props.put("client.id", "ProductorMSPrincipal");
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("security.protocol","SSL");
        props.put("ssl.truststore.location","/kafka-server-trustore.jks");
        props.put("ssl.truststore.password","c4_trust_d3f4ult_k3yst0r3");
        props.put("ssl.keystore.location","/client_kafka.jks");
        props.put("ssl.keystore.password","br0k3r_0_c0nflu3nt_k4fk4_s3c");
        props.put("ssl.key.password","br0k3r_0_c0nflu3nt_k4fk4_s3c");

        if (getProducer() == null)
        	producer = new KafkaProducer<>(props);
        this.topic = topic;
    }


    public void send(String body) throws ExecutionException, InterruptedException {
        getProducer().send(new ProducerRecord(topic, body)).get();
    }

	public KafkaProducer<Integer, String> getProducer() {
		return producer;
	}
}


