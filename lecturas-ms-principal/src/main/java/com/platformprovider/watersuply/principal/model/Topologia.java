package com.platformprovider.watersupply.principal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.domain.Persistable;

/**
 * Created by atarin on 03/05/17.
 */
@Data
public class Topologia {
	// private String codigo_mensaje;
	private String fecha;
	// private String fabricante;
	private String tipo_activo;
	private String tipo_activo_padre;
	private String serial_number;
	private String id;
	private String id_padre;
}
