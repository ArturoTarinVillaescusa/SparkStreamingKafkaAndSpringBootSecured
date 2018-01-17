package lecturas.consumers

import java.util.Properties

import funcionestodojunto._
import kafka.common.TopicAndPartition
import kafka.serializer.StringDecoder
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SaveMode
import org.apache.spark.streaming.kafka.KafkaUtils
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

      val kafkaDirectParams = Map(
        "metadata.broker.list" -> variablesAguas.kafkaBroker,
        "group.id" -> variablesAguas.grupoConsumidoresGenericos,
        "auto.offset.reset" -> "largest"
      )

      var fromOffsets : Map[TopicAndPartition, Long] = Map.empty
      val hdfsPath = variablesAguas.rutaLecturasHDFSParquet

      val kafkaDirectStream = KafkaUtils
        .createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaDirectParams, Set(topic))


		val id = kafkaDirectStream.map(_._2).map(parser)

		kafkaDirectStream.map(kv =>
			{
				val consulta = kv._2

				val resultado = consulta.substring(consulta.indexOf("{"))
				resultado
			}

		).foreachRDD(rdd =>
			if (rdd.toLocalIterator.nonEmpty) {

				val json = sqlContext.read.json(rdd)

				try {
					val dfTopologias = json.select(org.apache.spark.sql.functions.explode(json("topologias"))).toDF("topologias")

					val consultaTopologias = dfTopologias.select("topologias.fecha", "topologias.tipo_activo", "topologias.tipo_activo_padre",
														"topologias.serial_number", "topologias.id", "topologias.id_padre")

				  try {
					  // guardamos en HDFS
					  println("Guardando en HDFS los datos procesados ...")

					  consultaTopologias.write.partitionBy("tipo_activo").mode(SaveMode.Append)
						  .parquet(variablesAguas.rutaLecturasHDFSParquet + variablesAguas.tablaTopologias.replace("public.", "") + "/")
				  } catch {
					  case ex: Exception => println("Error guardando en HDFS:\n"+ex.printStackTrace())
				  }

				  try {
					// guardamos en POSTGRES
					println("Guardando en Postgres los datos procesados ...")

					// Revisar SparkCopyPostgres.scala
					// https://gist.github.com/longcao/bb61f1798ccbbfa4a0d7b76e49982f84

					consultaTopologias.write.mode(SaveMode.Append)
							.jdbc(variablesAguas.urlPostgres, variablesAguas.tablaTopologias, propsConexionPostgres)
				    } catch {
						case ex: Exception => println("Error guardando en PostGresSql:\n"+ex.printStackTrace())
					}

				} catch {
					case ex: Exception => {

						val dfLecturas = json.select(org.apache.spark.sql.functions.explode(json("lecturas"))).toDF("lecturas")

						val consultaLecturas = dfLecturas.select("lecturas.unidad", "lecturas.version", "lecturas.id",
							"lecturas.numero_serie", "lecturas.fecha", "lecturas.valor")

						val dfAlarmas = json.select("alarmas")

						val consultaAlarmas = dfAlarmas.select("alarmas.fecha_inicio", "alarmas.fecha_fin",
							"alarmas.alarma_general", "alarmas.alarma_bloqueo", "alarmas.alarma_desincronizacion",
							"alarmas.tension_bateria", "alarmas.meses_bateria",
							"alarmas.intervalo_tiempo_volumen_flujo_inverso", "alarmas.numero_arranques",
							"alarmas.intervalo_tiempo_numero_arranques", "alarmas.tiempo_sin_paso_agua",
							"alarmas.intervalo_tiempo__sin_paso_agua", "alarmas.presion", "alarmas.temperatura",
							"alarmas.alarma_fuga", "alarmas.alarma_bateria", "alarmas.volumen_flujo_inverso",
							"alarmas.alarma_flujo_inverso", "alarmas.alarma_subgasto", "alarmas.alarma_sobregasto",
							"alarmas.alarma_pico_consumo", "alarmas.alarma_fraude_magnetico")

						try {
							// guardamos en HDFS
							println("Guardando en HDFS los datos procesados ...")

							consultaLecturas.write.partitionBy("numero_serie").mode(SaveMode.Append)
								.parquet(variablesAguas.rutaLecturasHDFSParquet + variablesAguas.tablaLecturas.replace("public.", "") + "/")

							consultaAlarmas.write.partitionBy("intervalo_tiempo_volumen_flujo_inverso").mode(SaveMode.Append)
								.parquet(variablesAguas.rutaLecturasHDFSParquet + variablesAguas.tablaAlarmas.replace("public.", "") + "/")

						} catch {
							case ex: Exception => println("Error guardando en HDFS:\n"+ex.printStackTrace())
						}

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

					}
				}
			} else println("--------> " + System.currentTimeMillis())
		)

      ssc
    }

    val ssc = getStreamingContext(streamingApp, sc, batchDuration)

    ssc.start()
    ssc.awaitTermination()

  }
}
