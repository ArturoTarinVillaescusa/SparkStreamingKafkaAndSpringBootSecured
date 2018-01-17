CREATE TABLE LECTURAS (
	id_watersupply UUID varchar(255),
	id BIGSERIAL primary key,
	fabricante varchar(255),
	version varchar(255),
	numero_serie BIGINT,
	alarma_fuga BOOLEAN,
	alarma_bateria BOOLEAN,
	alarma_flujo_inverso BOOLEAN,
	unidad varchar(255),
	fecha varchar(255),
	valor varchar(255)
);