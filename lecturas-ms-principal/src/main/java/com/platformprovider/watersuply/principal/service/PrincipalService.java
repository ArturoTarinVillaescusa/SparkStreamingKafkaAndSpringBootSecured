package com.platformprovider.watersupply.principal.service;

import com.platformprovider.watersupply.principal.model.Lecturas;
import com.platformprovider.watersupply.principal.model.LecturasYAlarmas;
import com.platformprovider.watersupply.principal.model.Topologias;

import java.util.List;

/**
 * Created by atarin on 03/05/17.
 */
public interface PrincipalService {
	void insert(Lecturas lectura);

	void insertaLecturasYAlarmas(String lecturasYAlarmas);

	void insertaTopologias(String topologias);

	List<Lecturas> findAll();
}
