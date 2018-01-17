package com.platformprovider.watersupply.lecturas.contazara;

import com.platformprovider.watersupply.lecturas.common.MessaggingConstants;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

/**
 * Created by jalonso on 12/04/17.
 */
@Component
class ContazaraMessageSender {

	private final MessageChannel output;

	public ContazaraMessageSender(MessageChannel output) {
		this.output = output;
	}

	public void sendLectura(LecturaContazara lectura) {
		this.output.send(MessageBuilder.withPayload(lectura).setHeader(MessaggingConstants.LECTURA_HEADER, MessaggingConstants.LECTURA_CONTAZARA).build());
	}

}
