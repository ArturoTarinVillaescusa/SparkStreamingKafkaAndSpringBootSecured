package com.platformprovider.watersupply.principal.controller;

import com.platformprovider.watersupply.principal.model.Lecturas;
import com.platformprovider.watersupply.principal.model.LecturasYAlarmas;
import com.platformprovider.watersupply.principal.model.Topologias;
import com.platformprovider.watersupply.principal.service.PrincipalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Created by atarin on 03/05/17.
 */
@Slf4j
@RestController
class PrincipalController {

	private final PrincipalService principalService;

	public PrincipalController(PrincipalService principalService) {
		this.principalService = principalService;
	}

	@PostMapping("/lecturas")
	Lecturas crearLectura(@RequestBody Lecturas lectura) {
		principalService.insert(lectura);
		return lectura;
	}

	@GetMapping("/lecturas")
	Collection<Lecturas> obtenerLecturas() {
		return principalService.findAll();
	}

	@PostMapping("/lecturasyalarmas")
	ResponseEntity<String> crearLecturasYAlarmas(@RequestBody String lecturasYAlarmas) {
		principalService.insertaLecturasYAlarmas(lecturasYAlarmas);

		return new ResponseEntity<String>(lecturasYAlarmas, HttpStatus.OK);
	}

	@PostMapping("/topologias")
	ResponseEntity<String> crearTopologias(@RequestBody String topologias) {
		principalService.insertaTopologias(topologias);

		return new ResponseEntity<String>(topologias, HttpStatus.OK);
	}

}
