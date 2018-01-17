package com.platformprovider.watersupply.principal.service;

import com.platformprovider.watersupply.principal.model.Lecturas;
import com.platformprovider.watersupply.principal.model.LecturasYAlarmas;
import com.platformprovider.watersupply.principal.model.Topologias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by atarin on 03/05/17.
 */
@Slf4j
@Service
public class PrincipalServiceImpl implements PrincipalService {

	private final PrincipalMessageSender principalMessageSender;
	private final PrincipalRepository principalRepository;

	public PrincipalServiceImpl(PrincipalRepository principalRepository, PrincipalMessageSender principalMessageSender) {
		this.principalRepository = principalRepository;
		this.principalMessageSender = principalMessageSender;
	}



	@Override
	public void insert(Lecturas lectura) {
		// log.debug("Enviando mensaje a la cola de Kafka llamada 'lectura', con id {}", lectura.getId());
		// principalMessageSender.sendLectura(lectura);
		// log.debug("Mensaje con id {} enviado", lectura.getId());

		// Descomentar si el Microservicio debe guardar en Postgres
		// log.debug("Insertando lectura: {}", lectura);
		// principalRepository.save(lectura);
	}

	@Override
	public void insertaLecturasYAlarmas(String lecturasYAlarmas) {
		principalMessageSender.sendLecturasYAlarmas(lecturasYAlarmas);

	}

	@Override
	public void insertaTopologias(String topologias) {
		principalMessageSender.sendTopologias(topologias);

	}

	@Override
	public List<Lecturas> findAll() {
		log.debug("Obteniendo lecturas");
		return principalRepository.findAll();
	}
}
