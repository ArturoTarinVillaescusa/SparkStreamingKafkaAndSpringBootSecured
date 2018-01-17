package com.platformprovider.watersupply.lecturas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;

/**
 * Created by root on 18/04/17.
 */
@Slf4j
@EnableBinding({Source.class, Sink.class})
class StreamsConfiguration {

	// Creamos este listener en la POC para leer los mensajes del broker y mostrarlo en la consola,
	// asegurando que realmente se están enviando
	@StreamListener(Sink.INPUT)
	public void handleLectura(Object o) {
		log.info("...............Recibido mensaje {}", o);
	}

}
