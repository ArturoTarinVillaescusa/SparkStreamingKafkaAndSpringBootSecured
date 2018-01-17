package com.platformprovider.rest.kafka.producer;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class Main {
    public static void main(String[] args) {
        port(KafkaProperties.PORT);
        Producer pLecturas=new Producer("lecturas");
        Producer pTopologias=new Producer("topologias");
        post("/topologias", (req, res) -> {
            pTopologias.send(req.body());
            return "sending 1000 msgs to kafka";
        });
        post("/lecturasyalarmas", (req, res) -> {
            pLecturas.send(req.body());
            return "sending 1000 msgs to kafka";
        });
    }
}
