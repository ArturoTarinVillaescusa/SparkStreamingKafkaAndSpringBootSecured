package com.platformprovider.watersupply.lecturas.contazara;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * Created by jalonso on 11/04/17.
 */
@Data
public class LecturaContazara implements Persistable<Long> {

	private Long id;
	private Instant timestamp;
	private Long measure;

	@Override
	@JsonIgnore
	public boolean isNew() {
		return id == null;
	}
}
