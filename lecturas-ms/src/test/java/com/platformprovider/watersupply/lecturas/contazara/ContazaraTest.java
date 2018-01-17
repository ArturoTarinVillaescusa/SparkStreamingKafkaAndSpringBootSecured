package com.platformprovider.watersupply.lecturas.contazara;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jalonso on 17/04/17.
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContazaraTest {

	private ObjectMapper objectMapper = null;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private MessageCollector messageCollector;

	@Autowired
	private MessageChannel output;

	@Before
	public void setup() {
		// Creamos un mapper registrando el módulo para mapear tipos de java.time
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void insercionOK() throws IOException {

		HttpEntity<String> entity = new HttpEntity<>(
			"{\"id\":null, \"timestamp\":1492512048.122000000, \"measure\":31}",
			getHeaders());

		// Se invoca al endpoint de inserción de lecturas y comprobamos que nos devuelve la lectura con el
		// id autogenerado
		LecturaContazara result = this.restTemplate.postForObject("/contazara", entity, LecturaContazara.class);
		assertThat(result.getId()).isNotNull();


		// Comprobamos que se ha enviado un mensaje con la lectura al broker
		Message<String> received =  (Message<String>) messageCollector.forChannel(output).poll();
		LecturaContazara receivedLectura = objectMapper.readValue(received.getPayload(), LecturaContazara.class);
		assertThat(receivedLectura).isEqualTo(result);

		// Invocamos al endpoint de consulta de lecturas y comprobamos que efectivamente
		// se había insertado en BBDD
		LecturaContazara[] retrieved = this.restTemplate.getForObject("/contazara",
			LecturaContazara[].class);
		assertThat(retrieved.length == 1);
		assertThat(retrieved[0]).isEqualTo(result);
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

}
