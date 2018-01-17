package com.platformprovider.watersupply.lecturas.contazara;

import java.util.List;

/**
 * Created by jalonso on 11/04/17.
 */
public interface ContazaraService {
	void insert(LecturaContazara lectura);

	List<LecturaContazara> findAll();
}
