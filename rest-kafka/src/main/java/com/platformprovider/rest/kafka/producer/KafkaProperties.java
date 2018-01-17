package com.platformprovider.rest.kafka.producer;

/**
 * Created by arincon on 5/06/17.
 */
public class KafkaProperties {
    public static final String KAFKA_SERVER_URL = "broker-2.confluent-kafka-sec.mesos";
    public static final String KAFKA_SERVER_PORT = "9092";
    public static final int PORT = Integer.valueOf(System.getenv().get("PORT"));
}
