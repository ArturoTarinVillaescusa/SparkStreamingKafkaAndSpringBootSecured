package com.platformprovider.watersupply.principal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.domain.Persistable;

/**
 * Created by atarin on 03/05/17.
 */
@Data
public class Alarmas {

	private String fecha_inicio;
	private String fecha_fin;
	private String alarma_general;
	private String alarma_bloqueo;
	private String alarma_desincronizacion;
	private String tension_bateria;
	private String meses_bateria;
	private String intervalo_tiempo_volumen_flujo_inverso;
	private String numero_arranques;
	private String intervalo_tiempo_numero_arranques;
	private String tiempo_sin_paso_agua;
	private String intervalo_tiempo__sin_paso_agua;
	private String presion;
	private String temperatura;
	private String alarma_fuga;
	private String alarma_bateria;
	private String volumen_flujo_inverso;
	private String alarma_flujo_inverso;
	private String alarma_subgasto;
	private String alarma_sobregasto;
	private String alarma_pico_consumo;
	private String alarma_fraude_magnetico;
	private String alarma_desinstalacion;
	private String alarma_manipulacion;
	private String alarma_contador_invertido;
}
