package com.platformprovider.watersupply.principal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Created by atarin on 03/05/17.
 */
@Data
public class Lecturas implements Persistable<Long> {

	private UUID id_watersupply;
	private Long id;
	// private String fabricante;
	private String version;
	private Long numero_serie;
	// private Boolean alarma_fuga;
	// private Boolean alarma_bateria;
	// private Boolean alarma_flujo_inverso;
	private String unidad;
	private String fecha;
	private String valor;

	@Override
	@JsonIgnore
	public boolean isNew() {
		return id == null;
	}
}
