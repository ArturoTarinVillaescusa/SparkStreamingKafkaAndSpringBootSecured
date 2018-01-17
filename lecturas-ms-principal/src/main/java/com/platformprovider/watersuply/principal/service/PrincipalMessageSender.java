package com.platformprovider.watersupply.principal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platformprovider.watersupply.principal.config.ProductorKafka;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Created by atarin on 03/05/17.
 */
@Slf4j
@Component
class PrincipalMessageSender {

	/*
	private final MessageChannel output;

	public PrincipalMessageSender(MessageChannel output) {
		this.output = output;
	}

	public void sendLectura(Lecturas lectura) {
		this.output.send(MessageBuilder.withPayload(lectura).setHeader(MessaggingConstants.LECTURA_HEADER, MessaggingConstants.LECTURA_GENERICA).build());
	}

*/
	public void sendLecturasYAlarmas(String lecturasYAlarmas) {
		// log.debug("sendLecturasYAlarmas ::: "+MessageBuilder.withPayload(lecturasYAlarmas).setHeader(MessaggingConstants.LECTURA_HEADER, MessaggingConstants.LECTURA_GENERICA).build().toString());
		// insertaMensajeEnKafka(lecturasYAlarmas, "lecturas");

		try {
			JSONObject obj = new JSONObject(lecturasYAlarmas);

			JSONArray arrlecturas = obj.getJSONArray("lecturas");
			for (int i = 0; i < arrlecturas.length(); i++)
			{
				JSONArray arrDatolecturas = arrlecturas.getJSONObject(i).getJSONArray("Datolecturas");
				for (int j = 0; j < arrDatolecturas.length(); j++)
				{

					String lectura = arrDatolecturas.getJSONObject(j).toString();

					try {
						ProductorKafka pLecturas=new ProductorKafka("lecturas");

						ObjectMapper mapeadorJackson = new ObjectMapper();
						System.out.println("Processing received lectura Json from Microservice ......");
						System.out.println(mapeadorJackson.writerWithDefaultPrettyPrinter().writeValueAsString(lectura));

						System.out.println("Before sending lectura Json to Kafka en el instante "+System.currentTimeMillis());
						log.debug("Before sending lectura Json to Kafka en el instante "+System.currentTimeMillis());
						pLecturas.send(lectura);
						System.out.println("Sent lectura Json to Kafka en el instante "+System.currentTimeMillis());
						log.debug("Sent lectura Json to Kafka en el instante "+System.currentTimeMillis());
					} catch (Exception e) {

						log.debug("Error sendLecturasYAlarmas ::: "+e.toString());
						System.out.println("Error sendLecturasYAlarmas ::: "+e.toString());

						e.printStackTrace();
						log.debug("Mensaje que causó el error "+lectura);
						System.out.println("Mensaje que causó el error "+lectura);

					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendTopologias(String topologias) {
		// log.debug("sendTopologias ::: "+MessageBuilder.withPayload(topologias).setHeader(MessaggingConstants.LECTURA_HEADER, MessaggingConstants.LECTURA_GENERICA).build().toString());

		// insertaMensajeEnKafka(topologias, "topologias");

		ProductorKafka pTopologias=new ProductorKafka("topologias");

		System.out.println(".........pte");
		/*
		try {
			pTopologias.send(topologias);
			log.debug("Ending sendTopologias:  Microservice sent json to 'topologias' topic ...");
			System.out.println("Ending sendTopologias:  Microservice sent json to 'topologias' topic");
		} catch (Exception e) {
			log.debug("Error sendTopologias ::: "+e.toString());
			System.out.println("Error sendTopologias ::: "+e.toString());
		}
		*/

	}

}
