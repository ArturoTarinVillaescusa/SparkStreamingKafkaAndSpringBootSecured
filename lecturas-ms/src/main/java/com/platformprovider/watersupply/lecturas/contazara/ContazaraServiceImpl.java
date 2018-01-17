package com.platformprovider.watersupply.lecturas.contazara;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by jalonso on 12/04/17.
 */
@Service
@Slf4j
public class ContazaraServiceImpl implements ContazaraService {

	private final ContazaraMessageSender contazaraMessageSender;
	private final ContazaraRepository contazaraRepository;

	public ContazaraServiceImpl(ContazaraRepository contazaraRepository, ContazaraMessageSender contazaraMessageSender) {
		this.contazaraRepository = contazaraRepository;
		this.contazaraMessageSender = contazaraMessageSender;
	}


	@Override
	public void insert(LecturaContazara lectura) {
		log.debug("Insertando lectura: {}", lectura);
		contazaraRepository.save(lectura);
		log.debug("Enviando mensaje de lectura con id {}", lectura.getId());
		contazaraMessageSender.sendLectura(lectura);
		log.debug("Mensaje con id {} enviado", lectura.getId());
	}

	@Override
	public List<LecturaContazara> findAll() {
		log.debug("Obteniendo lecturas");
		return contazaraRepository.findAll();
	}
}
