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
  topicosappel

  Si no existe, lo creamos:
  ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topicosappel

  Enviamos al tópico un un archivo de lectura XML usando el productor:
  ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topicosappel < ~/Telelecturas/SAPPEL/Decodificado/82927890_161227_020600.csv

  Para enviar todos los archivos de lecturas usamos el siguiente script:
  for i in `find ~/Telelecturas/SAPPEL -name "*.csv" | grep -v .GP2 | grep -v .dat`;
    do ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topicosappel < $i;
  done
 *
 */
object ParseSappelJsonsArrivingFromKafkaStream {

  def main(args: Array[String]): Unit = {

    // Desactivamos logs de línea de comando
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    // setup spark context
    val sc = getSparkContext("ParseSappelJsonsArrivingFromKafkaStream")
    val sqlContext = getSQLContext(sc)
    import sqlContext.implicits._

    var propsConexionPostgres: Properties = new Properties()
    propsConexionPostgres.setProperty("user", variablesAguas.usuarioPostgres)
    propsConexionPostgres.setProperty("password", variablesAguas.clavePostgres)
    propsConexionPostgres.setProperty("driver", "org.postgresql.Driver")

    val batchDuration = Seconds(variablesAguas.ventanaSappel)

    def streamingApp(sc: SparkContext, batchDuration: Duration) = {
      val ssc = new StreamingContext(sc, batchDuration)
      val topic = variablesAguas.topicoTelelecturasSappel

      val kafkaDirectParams = Map(
        "metadata.broker.list" -> variablesAguas.kafkaBroker,
        "group.id" -> variablesAguas.grupoConsumidoresGenericos,
        "auto.offset.reset" -> "largest"
      )

      var fromOffsets : Map[TopicAndPartition, Long] = Map.empty
      val hdfsPath = variablesAguas.rutaLecturasHDFSParquet

      val kafkaDirectStream = KafkaUtils
        .createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaDirectParams, Set(topic))

      val sappelStream = kafkaDirectStream.transform(rddSappel => {
            transforma_json_df_a_RDD_Sappel(rddSappel)
          }
        )

      sappelStream.foreachRDD { rdd =>
        println("Transformando lecturas Sappel ...")

        if (!rdd.isEmpty()) {
          val lecturasSappelDF = rdd
            //.repartition(8)
            .toDF()

          lecturasSappelDF.createOrReplaceTempView("lecturasSappel")

          val lecturasSappelConsultadas = sqlContext.sql(
            """ select distinct * from lecturasSappel
              |where id_tRF <> ""
            """.stripMargin)

          lecturasSappelConsultadas.cache()
          lecturasSappelConsultadas.show()

          try {
            // guardamos en HDFS
            println("Guardando en HDFS los datos procesados ...")
            lecturasSappelConsultadas.write.partitionBy("fechalectura").mode(SaveMode.Append)
              .parquet(variablesAguas.rutaLecturasHDFSParquet + variablesAguas.tablaLecturasSappel.replace("public.", "") + "/")
          } catch {
            case ex: Exception => println("Error guardando en HDFS:\n"+ex.printStackTrace())
          }

          try {
            // guardamos en POSTGRES
            println("Guardando en Postgres los datos procesados ...")
            lecturasSappelConsultadas.write.mode(SaveMode.Append)
              .jdbc(variablesAguas.urlPostgres, variablesAguas.tablaLecturasSappel, propsConexionPostgres)
          } catch {
            case ex: Exception => println("Error guardando en PostGresSql:\n"+ex.printStackTrace())
          }
        } else {
          println("Esperando la llegada de lecturas Sappel a su cola de Kafka 'topicosappel' ...")
        }

      }

      ssc
    }

    val ssc = getStreamingContext(streamingApp, sc, batchDuration)

    ssc.start()
    ssc.awaitTermination()

  }
}
