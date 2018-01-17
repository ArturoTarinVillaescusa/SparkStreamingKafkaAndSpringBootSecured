package com.platformprovider.watersupply.principal.service;


import com.nurkiewicz.jdbcrepository.JdbcRepository;
import com.nurkiewicz.jdbcrepository.RowUnmapper;
import com.platformprovider.watersupply.principal.model.Lecturas;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by atarin on 03/05/17.
 */
@Slf4j
@Repository
class PrincipalRepository extends JdbcRepository<Lecturas, Long> {

	private static final RowMapper<Lecturas> ROW_MAPPER = (rs, rowNum) -> {
		Lecturas lectura = new Lecturas();
		lectura.setId(rs.getLong("id"));
		// lectura.setFabricante(rs.getString("fabricante"));
		lectura.setVersion(rs.getString("version"));
		lectura.setNumero_serie(rs.getLong("numero_serie"));
		// lectura.setAlarma_fuga(rs.getBoolean("alarma_fuga"));
		// lectura.setAlarma_bateria(rs.getBoolean("alarma_bateria"));
		// lectura.setAlarma_flujo_inverso(rs.getBoolean("alarma_flujo_inverso"));
		lectura.setUnidad(rs.getString("unidad"));
		lectura.setFecha(rs.getString("fecha"));
		lectura.setValor(rs.getString("valor"));

		return lectura;
	};

	private static final RowUnmapper<Lecturas> ROW_UNMAPPER = lectura -> {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", lectura.getId());
		// row.put("fabricante", lectura.getFabricante());
		row.put("version", lectura.getVersion());
		row.put("numero_serie", lectura.getNumero_serie());
		// row.put("alarma_fuga", lectura.getAlarma_fuga());
		// row.put("alarma_bateria", lectura.getAlarma_bateria());
		// row.put("alarma_flujo_inverso", lectura.getAlarma_flujo_inverso());
		row.put("unidad", lectura.getUnidad());
		row.put("fecha", lectura.getFecha());
		row.put("valor", lectura.getValor());

		return row;
	};

	public PrincipalRepository() {
		super(ROW_MAPPER, ROW_UNMAPPER, "lecturas");
	}

	@Override
	protected <S extends Lecturas> S postCreate(S entity, Number generatedId) {
		entity.setId(generatedId.longValue());
		return entity;
	}
}
