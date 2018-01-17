package com.platformprovider.watersupply.lecturas.contazara;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jalonso on 17/04/17.
 */
@RunWith(SpringRunner.class)
@JsonTest
public class ContazaraJsonTest {

	@Autowired
	private JacksonTester<LecturaContazara> json;

	@Test
	public void lecturaSerializaOK() throws IOException {

		LecturaContazara lecturaTest = getLecturaTest();
		assertThat(json.write(lecturaTest)).isEqualToJson("lecturaContazara.json");
	}

	@Test
	public void lecturaDeserializaOK() throws IOException {

		LecturaContazara leida = json.read("lecturaContazara.json").getObject();
		LecturaContazara lecturaTest = getLecturaTest();

		assertThat(leida).isEqualTo(lecturaTest);
		assertThat(leida.getMeasure()).isEqualTo(lecturaTest.getMeasure());
		assertThat(leida.getTimestamp()).isEqualTo(lecturaTest.getTimestamp());
	}

	private LecturaContazara getLecturaTest() {
		LecturaContazara l = new LecturaContazara();
		l.setTimestamp(Instant.ofEpochMilli(1492512048122L));
		l.setMeasure(31L);
		return l;
	}

}
