package com.platformprovider.watersupply.principal.model;

import lombok.Data;

import java.util.List;

/**
 * Created by atarin on 03/05/17.
 */
@Data
public class Topologias {

	String codigo_mensaje;
	String fabricante;
	List<Topologia> topologias;
}
