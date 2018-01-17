package com.platformprovider.watersupply.lecturas.contazara;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Created by jalonso on 11/04/17.
 */
@RestController
class ContazaraController {

	private final ContazaraService contazaraService;

	public ContazaraController(ContazaraService contazaraService) {
		this.contazaraService = contazaraService;
	}

	@PostMapping("/contazara")
	LecturaContazara crearLectura(@RequestBody LecturaContazara lectura) {
		contazaraService.insert(lectura);
		return lectura;
	}

	@GetMapping("/contazara")
	Collection<LecturaContazara> obtenerLecturas() {
		return contazaraService.findAll();
	}
}
