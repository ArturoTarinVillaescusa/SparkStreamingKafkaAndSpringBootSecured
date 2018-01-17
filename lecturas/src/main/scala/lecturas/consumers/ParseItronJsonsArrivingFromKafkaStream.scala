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
  Mostramos el tópico de Kafka que vamos a utilizar
  ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-topics.sh --list --zookeeper localhost:2181
  topicoitron

  Si no existe, lo creamos:
  ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topicoitron

  Enviamos al tópico un un archivo de lectura XML usando el productor:
  ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topicoitron < ~/Telelecturas/ITRON/tresRegistros.txt

  ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topicoitron < ~/Telelecturas/ITRON/expCZ_20161231_120003.txt

  Para enviar todos los archivos de lecturas usamos el siguiente script:
  for i in `find ~/Telelecturas/ITRON -name "*.txt" | grep -v .GP2 | grep -v .dat`;
    do ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topicoitron < $i;
  done
 *
 */
object ParseItronJsonsArrivingFromKafkaStream {

  def main(args: Array[String]): Unit = {

    // Desactivamos logs de línea de comando
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    // setup spark context
    val sc = getSparkContext("ParseItronJsonsArrivingFromKafkaStream")
    val sqlContext = getSQLContext(sc)
    import sqlContext.implicits._

    var propsConexionPostgres: Properties = new Properties()
    propsConexionPostgres.setProperty("user", variablesAguas.usuarioPostgres)
    propsConexionPostgres.setProperty("password", variablesAguas.clavePostgres)
    propsConexionPostgres.setProperty("driver", "org.postgresql.Driver")

    val batchDuration = Seconds(variablesAguas.ventanaItron)

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

      val itronStream = kafkaDirectStream.transform(rddItron => {
            transforma_json_df_a_RDD_Itron(rddItron)
          }
        )

      itronStream.foreachRDD { rdd =>
        println("Transformando lecturas Itron ...")

        if (!rdd.isEmpty()) {
          val lecturasItronDF = rdd
            //.repartition(8)
            .toDF()

          lecturasItronDF.createOrReplaceTempView("lecturasItron")

          val lecturasItronConsultadas = sqlContext.sql(
            """ select distinct * from lecturasItron
              |where _name <> ""
              |and route_ID <> ""
            """.stripMargin)

          lecturasItronConsultadas.cache()
          lecturasItronConsultadas.show()

          try {
            // guardamos en HDFS
            println("Guardando en HDFS los datos procesados ...")
            lecturasItronConsultadas.write.partitionBy("route_ID").mode(SaveMode.Append)
              .parquet(variablesAguas.rutaLecturasHDFSParquet + variablesAguas.tablaLecturasItron.replace("public.", "") + "/")
          } catch {
            case ex: Exception => println("Error guardando en HDFS:\n"+ex.printStackTrace())
          }

          try {
            // guardamos en POSTGRES
            println("Guardando en Postgres los datos procesados ...")
            lecturasItronConsultadas.write.mode(SaveMode.Append)
              .jdbc(variablesAguas.urlPostgres, variablesAguas.tablaLecturasItron, propsConexionPostgres)
          } catch {
            case ex: Exception => println("Error guardando en PostGresSql:\n"+ex.printStackTrace())
          }
        } else {
          println("Esperando la llegada de lecturas Itron a su cola de Kafka 'topicoitron' ...")
        }

      }

      ssc
    }

    val ssc = getStreamingContext(streamingApp, sc, batchDuration)

    ssc.start()
    ssc.awaitTermination()

  }
}
