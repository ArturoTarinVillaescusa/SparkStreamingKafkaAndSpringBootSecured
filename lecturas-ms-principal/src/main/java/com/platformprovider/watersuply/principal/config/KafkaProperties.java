package com.platformprovider.watersupply.principal.config;

/**
 * Created by arincon on 5/06/17.
 */
public class KafkaProperties {
	public static final String KAFKA_SERVER_URL = "broker-2.confluent-kafka-sec.mesos";
	// Docker local
	// public static final String KAFKA_SERVER_URL = "172.17.0.4";
    public static final String KAFKA_SERVER_PORT = "9092";
    public static final int PORT = Integer.valueOf(System.getenv().get("PORT"));
}
