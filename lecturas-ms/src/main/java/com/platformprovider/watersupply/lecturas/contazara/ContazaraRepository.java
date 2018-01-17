package com.platformprovider.watersupply.lecturas.contazara;


import com.nurkiewicz.jdbcrepository.JdbcRepository;
import com.nurkiewicz.jdbcrepository.RowUnmapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jalonso on 11/04/17.
 */
@Repository
class ContazaraRepository extends JdbcRepository<LecturaContazara, Long> {

	private static final RowMapper<LecturaContazara> ROW_MAPPER = (rs, rowNum) -> {
		LecturaContazara lectura = new LecturaContazara();
		lectura.setId(rs.getLong("id"));
		lectura.setTimestamp(rs.getTimestamp("timestamp").toInstant());
		lectura.setMeasure(rs.getLong("measure"));
		return lectura;
	};

	private static final RowUnmapper<LecturaContazara> ROW_UNMAPPER = lectura -> {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", lectura.getId());
		row.put("timestamp", Timestamp.from(lectura.getTimestamp()));
		row.put("measure", lectura.getMeasure());
		return row;
	};

	public ContazaraRepository() {
		super(ROW_MAPPER, ROW_UNMAPPER, "lecturas");
	}

	@Override
	protected <S extends LecturaContazara> S postCreate(S entity, Number generatedId) {
		entity.setId(generatedId.longValue());
		return entity;
	}
}
