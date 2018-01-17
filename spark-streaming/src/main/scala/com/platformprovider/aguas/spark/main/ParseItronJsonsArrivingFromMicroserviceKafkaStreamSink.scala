package com.platformprovider.aguas.spark.main

import java.util.Properties

import com.fasterxml.jackson.databind.ObjectMapper
import funcionessparkstreaming._
import kafka.common.TopicAndPartition
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SaveMode
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Duration, Seconds, StreamingContext}
import utiles.SparkUtiles._

/*
  El tópico de Kafka que vamos a utilizar es el que utiliza el microservicio 'lecturas-ms-principal'
  para depositar los Json de lecturas y de topologia

En desarrollo se puede encontrar en la máquina virtual watersupply-kafka:

atarin@atarin:~$ sudo docker exec -it watersupply-kafka /bin/bash
root@watersupply-kafka:/#

root@watersupply-kafka:/# ./opt/kafka_2.11-0.10.1.0/bin/kafka-topics.sh --list --zookeeper localhost:2181
__consumer_offsets
lecturas
...
 *
 */
object ParseItronJsonsArrivingFromMicroserviceKafkaStreamSink {



	def main(args: Array[String]): Unit = {

    // Desactivamos logs de línea de comando
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    // setup spark context
    val sc = getSparkContext(variablesAguas.appParseJsonsFromMicroserviceSink)
    val sqlContext = getSQLContext(sc)

    var propsConexionPostgres: Properties = new Properties()
    propsConexionPostgres.setProperty("user", variablesAguas.usuarioPostgres)
    propsConexionPostgres.setProperty("password", variablesAguas.clavePostgres)
    propsConexionPostgres.setProperty("driver", "org.postgresql.Driver")

    val batchDuration = Seconds(variablesAguas.ventanaItron)

	  def parser(json: String): Any = {
			println(json)
	  }

	def streamingApp(sc: SparkContext, batchDuration: Duration) = {
      val ssc = new StreamingContext(sc, batchDuration)
      val topic = variablesAguas.topicoLecturasFromMsPrincipal
		val topics: Array[String] = Array(topic)

      val kafkaDirectParams:Map[String,String] = Map(
        "bootstrap.servers" -> String.valueOf(variablesAguas.kafkaBroker),
        "group.id" -> String.valueOf(variablesAguas.grupoConsumidoresGenericos),
		"key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
		"value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer"
      )

		val sparkConf = sc.getConf

		val params:Map[String, String] = if(sparkConf.contains("spark.secret.kafka.security.protocol")) {
			kafkaDirectParams ++ sparkConf.getAll.flatMap{case (key, value) =>
				if(key.startsWith("spark.secret.kafka.")) {
					Option((key.split("spark.secret.kafka.").tail.head.toLowerCase, value))
				} else None
			}
		} else kafkaDirectParams

      var fromOffsets : Map[TopicAndPartition, Long] = Map.empty
      val hdfsPath = variablesAguas.rutaLecturasHDFSParquet

      val kafkaDirectStream = KafkaUtils
        .createDirectStream[String, String](ssc,PreferConsistent,Subscribe[String,String]
		  (topics,params))


		val id = kafkaDirectStream.map(_.value()).map(parser)

		kafkaDirectStream.map(kv =>
			{
				val consulta = kv.value()
				consulta
			}

		).foreachRDD(rdd =>
			if (rdd.toLocalIterator.nonEmpty) {

				val json = sqlContext.read.json(rdd)

				val mapeadorJackson = new ObjectMapper
				println("Procesando Json de Lecturas recibido desde el Microservicio ......")
				println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(json))

				/*
				CREATE TABLE watersupply.lectura (
id uuid NOT NULL,
activo_codigo_hash varchar(150) NOT NULL DEFAULT '0'::character varying,
punto_suministro_id int8 NULL,
fecha_lectura timestamptz NOT NULL,
valor numeric(18,6) NOT NULL,
"precision" varchar(4) NOT NULL,
origen_lectura_id int2 NOT NULL,
metodo_lectura_id int2 NULL,
invalida bool NOT NULL,
usuario varchar(50) NOT NULL,
fecha_creacion timestamptz NOT NULL,
borrado bool NOT NULL DEFAULT false,
observacion_lectura_id int2 NULL,
orden_trabajo_id int8 NULL,
orden_trabajo_lectura_id int2 NULL,
CONSTRAINT "CheckConstraint1" CHECK ((("precision")::text = ANY (ARRAY[('m3'::character varying)::text, ('l'::character varying)::text, ('ml'::character varying)::text, ('cl'::character varying)::text, ('dl'::character varying)::text, ('dal'::character varying)::text, ('hl'::character varying)::text, ('dam3'::character varying)::text]))),
CONSTRAINT pk_lectura PRIMARY KEY (activo_codigo_hash,id),
CONSTRAINT metodo_lectura_lectura FOREIGN KEY (metodo_lectura_id) REFERENCES watersupply.metodo_lectura(id),
CONSTRAINT observacion_lectura_lectura FOREIGN KEY (observacion_lectura_id) REFERENCES watersupply.observacion_lectura(id),
CONSTRAINT origen_lectura_lectura FOREIGN KEY (origen_lectura_id) REFERENCES watersupply.origen_lectura(id),
CONSTRAINT punto_suministro_lectura_0 FOREIGN KEY (punto_suministro_id) REFERENCES watersupply.punto_suministro(id)
)
WITH (
OIDS=FALSE
) ;
CREATE INDEX "IX_Relationship2" ON watersupply.lectura (orden_trabajo_id DESC,orden_trabajo_lectura_id DESC) ;
CREATE INDEX ix_relationship2 ON watersupply.lectura (punto_suministro_id DESC) ;
CREATE INDEX ix_relationship56 ON watersupply.lectura (metodo_lectura_id DESC) ;
CREATE INDEX ix_relationship58 ON watersupply.lectura (observacion_lectura_id DESC) ;
CREATE INDEX ix_relationship66 ON watersupply.lectura (origen_lectura_id DESC) ;
				 */
				val consultaLecturas = json.select("fecha", "id", "id_watersupply", "numero_serie", "precision", "unidad", "valor", "version")

				consultaLecturas.printSchema()
				consultaLecturas.show()

				try {
					// guardamos en POSTGRES
					println("Guardando en Postgres los datos procesados ...")

					// Revisar SparkCopyPostgres.scala
					// https://gist.github.com/longcao/bb61f1798ccbbfa4a0d7b76e49982f84

					consultaLecturas.write.mode(SaveMode.Append)
						.jdbc(variablesAguas.urlPostgres, variablesAguas.tablaLecturas, propsConexionPostgres)

					println("Json de Lecturas almacenado en PostGres SQL:")
					println(json)
				} catch {
					case ex: Exception => println("Error guardando en PostGresSql:\n"+ex.printStackTrace())
				}

				try {

				} catch {
					case ex: Exception => {

						println("Error en ParseItronJsonsArrivingFromMicroserviceKafkaStreamSink ::: " +
							"streamingApp ::: if (rdd.toLocalIterator.nonEmpty)")
						println(ex.printStackTrace())

					}
				}

				/*
				try {
					val dfLecturas = json.select(org.apache.spark.sql.functions.explode(json("lecturas"))).toDF("lecturas")

					val consultaLecturas = dfLecturas.select("lecturas.id_watersupply", "lecturas.unidad", "lecturas.version", "lecturas.id",
						"lecturas.numero_serie", "lecturas.fecha", "lecturas.valor")

					consultaLecturas.printSchema()
					consultaLecturas.show()

					val dfAlarmas = json.select("alarmas")

					val consultaAlarmas = dfAlarmas.select("alarmas.fecha_inicio", "alarmas.fecha_fin",
						"alarmas.alarma_general", "alarmas.alarma_bloqueo", "alarmas.alarma_desincronizacion",
						"alarmas.tension_bateria", "alarmas.meses_bateria",
						"alarmas.intervalo_tiempo_volumen_flujo_inverso", "alarmas.numero_arranques",
						"alarmas.intervalo_tiempo_numero_arranques", "alarmas.tiempo_sin_paso_agua",
						"alarmas.intervalo_tiempo__sin_paso_agua", "alarmas.presion", "alarmas.temperatura",
						"alarmas.alarma_fuga", "alarmas.alarma_bateria", "alarmas.volumen_flujo_inverso",
						"alarmas.alarma_flujo_inverso", "alarmas.alarma_subgasto", "alarmas.alarma_sobregasto",
						"alarmas.alarma_pico_consumo", "alarmas.alarma_fraude_magnetico", "alarmas.alarma_desinstalacion",
						"alarmas.alarma_manipulacion", "alarmas.alarma_contador_invertido")

					consultaAlarmas.printSchema()
					consultaAlarmas.show()

					try {
						// guardamos en POSTGRES
						println("Guardando en Postgres los datos procesados ...")

						// Revisar SparkCopyPostgres.scala
						// https://gist.github.com/longcao/bb61f1798ccbbfa4a0d7b76e49982f84

						consultaLecturas.write.mode(SaveMode.Append)
							.jdbc(variablesAguas.urlPostgres, variablesAguas.tablaLecturas, propsConexionPostgres)

						consultaAlarmas.write.mode(SaveMode.Append)
							.jdbc(variablesAguas.urlPostgres, variablesAguas.tablaAlarmas, propsConexionPostgres)
					} catch {
						case ex: Exception => println("Error guardando en PostGresSql:\n"+ex.printStackTrace())
					}

				} catch {
					case ex: Exception => {

						println(ex.printStackTrace())
						println("Error en ParseItronJsonsArrivingFromMicroserviceKafkaStreamSink ::: " +
								"streamingApp ::: if (rdd.toLocalIterator.nonEmpty)")
					}
				}

				*/
			} else println("--------> " + System.currentTimeMillis())
		)

      ssc
    }

    val ssc = getStreamingContext(streamingApp, sc, batchDuration)

    ssc.start()
    ssc.awaitTermination()

  }
}
