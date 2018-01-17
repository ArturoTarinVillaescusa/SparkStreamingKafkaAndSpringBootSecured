package configuracion

import com.typesafe.config.ConfigFactory

/**
  * Created by atarin on 8/02/17.
  */
object VariablesCentralizadas {
  private val config = ConfigFactory.load()

  object VariablesTelelecturas {
    private val varTelelecturas = config.getConfig("lecturas")

    lazy val rutaOrigen = varTelelecturas.getString("ruta_origen")
    lazy val rutaLecturasHDFSParquet = varTelelecturas.getString("ruta_destino")
    lazy val rutaLecturasCrudasHDFS = varTelelecturas.getString("ruta_destino_csv")
    lazy val checkpointHDFS = varTelelecturas.getString("checkpoint_hdfs")
    lazy val checkpointLocal = varTelelecturas.getString("checkpoint_local")
    lazy val masterLocal = varTelelecturas.getString("master_local")
    lazy val masterMesos = varTelelecturas.getString("master_mesos")
    lazy val masterYarn = varTelelecturas.getString("master_yarn")
    lazy val extensionItron = varTelelecturas.getString("extension_itron")
    lazy val extensionContazara = varTelelecturas.getString("extension_contazara")
    lazy val extensionSappel = varTelelecturas.getString("extension_sappel")
    lazy val extensionIkor = varTelelecturas.getString("extension_ikor")
    lazy val ftpUrl = varTelelecturas.getString("ftp_url")
    lazy val ftpPort = varTelelecturas.getInt("ftp_port")
    lazy val ftpUser = varTelelecturas.getString("ftp_user")
    lazy val ftpPass = varTelelecturas.getString("ftp_password")
    lazy val ftpRemoteFolder = varTelelecturas.getString("ftp_remote_folder")
    lazy val ftpLocalFolder = varTelelecturas.getString("ftp_local_folder")
    lazy val urlMysqlAbering = varTelelecturas.getString("url_mysql_abering")
    lazy val usuarioMysqlAbering = varTelelecturas.getString("usuario_mysql_abering")
    lazy val claveMysqlAbering = varTelelecturas.getString("clave_mysql_abering")
    lazy val kafkaBroker = varTelelecturas.getString("kafka_broker")
    lazy val kafkaKeySerializerClassConfig = varTelelecturas.getString("kafka_key_serializer_class_config")
    lazy val kafkaValueSerializerClassConfig = varTelelecturas.getString("kafka_value_serializer_class_config")
    lazy val topicoTelelecturasItron = varTelelecturas.getString("kafka_topic_itron")
    lazy val topicoTelelecturasSensus = varTelelecturas.getString("kafka_topic_sensus")
    lazy val topicoTelelecturasSappel = varTelelecturas.getString("kafka_topic_sappel")
    lazy val topicoTelelecturasContazara = varTelelecturas.getString("kafka_topic_contazara")
    lazy val topicoTelelecturasIkor = varTelelecturas.getString("kafka_topic_ikor")
	lazy val topicoLecturasFromMsPrincipal = varTelelecturas.getString("kafka_topic_lecturas_from_ms_principal")
    lazy val grupoConsumidoresGenericos = varTelelecturas.getString("grupo_consumidores_genericos")
    lazy val ventanaItron = varTelelecturas.getInt("ventana_itron")
    lazy val ventanaSensus = varTelelecturas.getInt("ventana_sensus")
    lazy val ventanaSappel = varTelelecturas.getInt("ventana_sappel")
    lazy val ventanaContazara = varTelelecturas.getInt("ventana_contazara")
    lazy val ventanaIkor = varTelelecturas.getInt("ventana_ikor")
    lazy val urlPostgres = varTelelecturas.getString("url_postgres")
    lazy val usuarioPostgres = varTelelecturas.getString("usuario_postgres")
    lazy val clavePostgres = varTelelecturas.getString("clave_postgres")
    lazy val tablaLecturasSensus = varTelelecturas.getString("tabla_sensus")
    lazy val tablaLecturasSappel = varTelelecturas.getString("tabla_sappel")
    lazy val tablaLecturasContazara = varTelelecturas.getString("tabla_contazara")
    lazy val tablaLecturasIkor = varTelelecturas.getString("tabla_ikor")
    lazy val tablaLecturasIkorRSSI = varTelelecturas.getString("tabla_ikor_rssi")
    lazy val tablaLecturasIkorIDC = varTelelecturas.getString("tabla_ikor_idc")
    lazy val tablaLecturasIkorIDD = varTelelecturas.getString("tabla_ikor_idd")
    lazy val tablaLecturasItron = varTelelecturas.getString("tabla_itron")
	lazy val tablaLecturas = varTelelecturas.getString("tabla_lecturas")
    lazy val tablaAlarmas = varTelelecturas.getString("tabla_alarmas")
    lazy val tablaTopologias = varTelelecturas.getString("tabla_topologias")
	lazy val restEndpointLecturasPrincipal = varTelelecturas.getString("rest_endpoint_lecturas_principal")
  	lazy val restEndpointTopologiaPrincipal = varTelelecturas.getString("rest_endpoint_topologia_principal")
	lazy val appParseJsonsFromMicroserviceSink = varTelelecturas.getString("app_ParseItronJsonsArrivingFromMicroserviceKafkaStreamSink")
  }
}
