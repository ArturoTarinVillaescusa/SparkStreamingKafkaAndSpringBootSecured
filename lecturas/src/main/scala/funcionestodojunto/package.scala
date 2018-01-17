import java.io.{File, _}
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}

import clases._
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import configuracion.VariablesCentralizadas
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, StringRequestEntity}
import org.apache.commons.io.IOUtils
import org.apache.commons.net.ftp.{FTP, FTPClient}
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerConfig, ProducerRecord}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import utiles.SparkUtiles
import java.text.SimpleDateFormat
import java.util.Calendar

import utiles.SparkUtiles.getSparkContext


/**
  * Created by atarin on 8/02/17.
  *
  * Son las funciones no pertenecientes a la carga batch y Spark Streaming por lineas
  *
  *
  *
  */
package object funcionestodojunto {

	val variablesAguas = VariablesCentralizadas.VariablesTelelecturas

	/*
    Puede verificarse la inserción del registro utilizando un consumidor en línea de comando, por ejemplo

    atarin@atarin:~$ ~/Descargas/kafka_2.10-0.9.0.1/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic topicosappel from-beginning

     */
	def insertaMensajeEnKafka(linea: String, kafkaTopic : String)  = {

		val props = new Properties()

		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, variablesAguas.kafkaBroker)
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, variablesAguas.kafkaKeySerializerClassConfig)
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, variablesAguas.kafkaValueSerializerClassConfig)
		props.put(ProducerConfig.ACKS_CONFIG, "all")
		props.put(ProducerConfig.CLIENT_ID_CONFIG, "ProductorKafka")

		val kafkaProducer: Producer[Nothing, String] = new KafkaProducer[Nothing, String](props)
		println(kafkaProducer.partitionsFor(kafkaTopic))

		val producerRecord = new ProducerRecord(kafkaTopic, linea)
		kafkaProducer.send(producerRecord)
	}

	def enviaJsonAMicroservicioPrincipal(mensajeJson: String, restEndpointMsPrincipal: String) = {

		/*
		curl -H "Content-Type: application/json" -X POST -d '{ "lecturas": [{"codigo_mensaje" : "0001","fabricante" : "ITRON","guardar" :"true","version":"MiuCybleEverBluV2_1","unidad":"m3","id":"67566","numero_serie":"110327906","fecha":"2017-03-31 21:00:00","valor":1057.883},{"codigo_mensaje":"0001","fabricante":"ITRON","guardar":"true","version":"MiuCybleEverBluV2_1","unidad":"m3","id":"67566","numero_serie":"110327906","fecha":"2017-03-31 22:00:00","valor":1057.896}], "alarmas": {"fecha_inicio":"","fecha_fin":"","alarma_general":"","alarma_bloqueo":"","alarma_desincronizacion":"","tension_bateria":"","meses_bateria":"","intervalo_tiempo_volumen_flujo_inverso":"86400","numero_arranques":"","intervalo_tiempo_numero_arranques":"","tiempo_sin_paso_agua":"","intervalo_tiempo__sin_paso_agua":"","presion":"","temperatura":"","alarma_fuga":"true","alarma_bateria":"false","volumen_flujo_inverso":"0","alarma_flujo_inverso":"false","alarma_subgasto":"false","alarma_sobregasto":"false","alarma_pico_consumo":"false","alarma_fraude_magnetico":"false"}}' http://localhost:8080/lecturasyalarmas

		*/
		val httpClient : HttpClient = new HttpClient()

		val requestEntity = new StringRequestEntity(mensajeJson, "application/json", "UTF-8")

		val postMethod = new PostMethod(restEndpointMsPrincipal)
		postMethod.setRequestEntity(requestEntity)

		val statusCode = httpClient.executeMethod(postMethod)

		val responseBody = postMethod.getResponseBody()

		println(responseBody)

		postMethod.releaseConnection()

	}


	def procesaArchivosDeLecturaUbicadosEnRutaFTP(ftpClient: FTPClient, ftpRemoteFolder: String) : Unit = {

		val subFiles = ftpClient.listFiles(ftpRemoteFolder)

		if (subFiles.nonEmpty) {
			for (aFile <- subFiles) {

				val currentFileName = aFile.getName

				if (currentFileName != "." && currentFileName != ".." &&
					!currentFileName.toUpperCase.endsWith(".DAT") &&
					!currentFileName.toUpperCase.endsWith(".GP2")
				) {

					if (aFile.isDirectory) {

						procesaArchivosDeLecturaUbicadosEnRutaFTP(ftpClient, ftpRemoteFolder + "/" + currentFileName)

					} else {
						parseaCamposArchivoTelelecturas(ftpRemoteFolder + "/" + currentFileName)

					}
				}
			}
		}
	}

	def separaEn24lecturas(mensajeLecturas: ObjectNode) = {
		val mapeadorJackson = new ObjectMapper

		val jsonLecturasSeparadas = mapeadorJackson.createArrayNode

		var parteConstanteMensajeLectura = mapeadorJackson.createObjectNode

		val camposMensajeLecturas = mensajeLecturas.fields

		var pulsosPorHora : Array[Int] = new Array[Int](24)

		var consumoALas00, peso = 0.0

		var fecha = ""

		while(camposMensajeLecturas.hasNext()){
			var campo = camposMensajeLecturas.next()

			campo.getKey match {

				case "Pulsos00_01" => pulsosPorHora(0) = campo.getValue.asInt()
				case "Pulsos01_02" => pulsosPorHora(1) = campo.getValue.asInt()
				case "Pulsos02_03" => pulsosPorHora(2) = campo.getValue.asInt()
				case "Pulsos03_04" => pulsosPorHora(3) = campo.getValue.asInt()
				case "Pulsos04_05" => pulsosPorHora(4) = campo.getValue.asInt()
				case "Pulsos05_06" => pulsosPorHora(5) = campo.getValue.asInt()
				case "Pulsos06_07" => pulsosPorHora(6) = campo.getValue.asInt()
				case "Pulsos07_08" => pulsosPorHora(7) = campo.getValue.asInt()
				case "Pulsos08_09" => pulsosPorHora(8) = campo.getValue.asInt()
				case "Pulsos09_10" => pulsosPorHora(9) = campo.getValue.asInt()
				case "Pulsos10_11" => pulsosPorHora(10) = campo.getValue.asInt()
				case "Pulsos11_12" => pulsosPorHora(11) = campo.getValue.asInt()
				case "Pulsos12_13" => pulsosPorHora(12) = campo.getValue.asInt()
				case "Pulsos13_14" => pulsosPorHora(13) = campo.getValue.asInt()
				case "Pulsos14_15" => pulsosPorHora(14) = campo.getValue.asInt()
				case "Pulsos15_16" => pulsosPorHora(15) = campo.getValue.asInt()
				case "Pulsos16_17" => pulsosPorHora(16) = campo.getValue.asInt()
				case "Pulsos17_18" => pulsosPorHora(17) = campo.getValue.asInt()
				case "Pulsos18_19" => pulsosPorHora(18) = campo.getValue.asInt()
				case "Pulsos19_20" => pulsosPorHora(19) = campo.getValue.asInt()
				case "Pulsos20_21" => pulsosPorHora(20) = campo.getValue.asInt()
				case "Pulsos21_22" => pulsosPorHora(21) = campo.getValue.asInt()
				case "Pulsos22_23" => pulsosPorHora(22) = campo.getValue.asInt()
				case "Pulsos23_24" => pulsosPorHora(23) = campo.getValue.asInt()
				case "valor" => consumoALas00 = campo.getValue.asDouble()
				case "peso" => peso = campo.getValue.asDouble()
				case "fecha" => fecha = campo.getValue.asText()
				case _ => parteConstanteMensajeLectura.put(campo.getKey, campo.getValue)

			}
		}

		(1 until 24).foreach(i => {
			pulsosPorHora(i) = pulsosPorHora(i) + pulsosPorHora(i - 1)
		})

		(0 until 24).foreach(i => {
			var mensajeLectura  = mapeadorJackson.createObjectNode

			val registrosParteConstanteMensajeLectura = parteConstanteMensajeLectura.fields()
			while (registrosParteConstanteMensajeLectura.hasNext) {
				val registro = registrosParteConstanteMensajeLectura.next()

				mensajeLectura.put(registro.getKey, registro.getValue)
			}

			// Añadimos un día a la última de las 24 lecturas diarias
			if (i.equals(23)) {
				try {
					val sdf = new SimpleDateFormat("yyyy-MM-dd")
					val c = Calendar.getInstance
					c.setTime(sdf.parse(fecha))
					c.add(Calendar.DATE, 1)

					fecha = sdf.format(c.getTime)

				} catch {
					case e: Exception => {
						println(e.printStackTrace())
					}
				}
			}

			var fechaHora = ""
			if (i < 10)
				fechaHora = fecha + " 0"+(i+1)+":00:00"
			else if (i >=10 && i < 23)
				fechaHora = fecha + " "+(i+1)+":00:00"
			else
				fechaHora = fecha + " 00:00:00"

			mensajeLectura.put("fecha", fechaHora)
			mensajeLectura.put("valor", consumoALas00 + pulsosPorHora(i) * peso )

			jsonLecturasSeparadas.add(mensajeLectura)

		}
		)

		// println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturasSeparadas))
		jsonLecturasSeparadas

	}

	/*
        Usamos el mismo parseador de xml que utiliza Databricks. Para CSV he preferido utilizar un bucle.
        Para genear el mensaje JSon utilizo Jackson, que es más versátil que la biblioteca 	que utiliza Databricks
         */
	def deArchivoOrigenAMensajeJson(contenidoArchivo: String, marca: String, nombreArchivo: String) = {

		val mapeadorJackson = new ObjectMapper
		// var jsonDelXML = mapeadorJackson.createArrayNode
		val jsonVectorTopologias = mapeadorJackson.createArrayNode

		var jsonTopologias = mapeadorJackson.createObjectNode

		val jsonLecturas = mapeadorJackson.createArrayNode

		var jsonLecturas1XML = mapeadorJackson.createObjectNode

		marca match {

			case "ITRON" => {
				val reader = new StringReader(contenidoArchivo)
				val factory : XMLInputFactory = XMLInputFactory.newInstance() // Or newFactory()
				var xmlReader : XMLStreamReader = factory.createXMLStreamReader(reader)

				// Mensaje de Topología es un grupo de mensajes compuesto por:

				// Gateway: es el concentrador, el que genera el archivo xml que estamos analizando
				// Su etiquetaestá en un único sitio, y es la siguiente
				// <AccessPoint SerialNumber="164000159"

				// Colector: está repetido en muchos sitios, puede tener o no padre:
				// * si lo tiene es porque reporta a otro colector
				// * si no lo tiene es porque reporta directametne al Gateway

				// Obtenemos estos datos de las siguientes etiquetas:
				// <Collector SerialNumber="163002218" ParentSerialNumber="163002249" />
				//  <MiuCybleEverBluV2_1 ID="318519" CollectorSerialNumber="163002220" SerialNumber="160669051">

				var mensajeTopologia = mapeadorJackson.createObjectNode
				mensajeTopologia.put("codigo_mensaje", "0002")
				mensajeTopologia.put("fabricante", "ITRON")

				var fechaMensajeTopologia = ""
				var tipo_activo = ""
				var tipo_activo_padre = "gateway"
				var posicion10_SerialNumberDeAccessPoint_idGateway = ""
				var posicion16SerialNumberDeCollector = ""
				var posicion17ParentSerialNumberDeCollector = ""
				var posicion26idDeMiuCybleEverBluV2_1 = ""
				var posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1 = ""
				var posicion28SerialNumberDeMiuCybleEverBluV2_1 = ""

				var mensajeLecturas = mapeadorJackson.createObjectNode
				var informacionAdicional = mapeadorJackson.createObjectNode
				var IndexAtZeroHourInCubicMetre : String = "-1"
				var IndexAtZeroHourInPulses : String = "-1"
				var PulseWeight : String = "-1"

				while(xmlReader.hasNext()){

					var event = xmlReader.next()

					try {
						event match {

							case XMLStreamConstants.CHARACTERS => // println(xmlReader.getText().trim())

							case XMLStreamConstants.END_ELEMENT =>
								(0 until xmlReader.getAttributeCount).foreach(i => {
									//println(xmlReader.getLocalName +": <end_element>   "+xmlReader.getAttributeName(i) + " = " + xmlReader.getAttributeValue(i))
								})

							case XMLStreamConstants.START_ELEMENT => {

								if ("AccessPoint".equals(xmlReader.getLocalName))
									(0 until xmlReader.getAttributeCount).foreach(i => {

											if ("SerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
												// mensajeTopologia.put("gatewaySN", xmlReader.getAttributeValue(i) )
												posicion10_SerialNumberDeAccessPoint_idGateway = xmlReader.getAttributeValue(i)
											}
										}
										)

								if ("RouteFdcSettings".equals(xmlReader.getLocalName))
									(0 until xmlReader.getAttributeCount).foreach(i => {

										if ("EndDataDate".equals(xmlReader.getAttributeName(i).toString)) {
											fechaMensajeTopologia = xmlReader.getAttributeValue(i)
										}
									}
									)

								if ("Collector".equals(xmlReader.getLocalName))
									(0 until xmlReader.getAttributeCount).foreach(i => {

										if ("SerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
											tipo_activo = "colector"
											posicion16SerialNumberDeCollector = xmlReader.getAttributeValue(i)
										}
										if ("ParentSerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
											tipo_activo = "modulo"
											tipo_activo_padre = "colector"
											posicion17ParentSerialNumberDeCollector = xmlReader.getAttributeValue(i)
										}

									}
									)

								if ("PulseValueUnit".equals(xmlReader.getLocalName))
									(0 until xmlReader.getAttributeCount).foreach(i => {
										if ("PulseWeight".equals(xmlReader.getAttributeName(i).toString)) {
											PulseWeight = xmlReader.getAttributeValue(i)
											mensajeLecturas.put("peso", BigDecimal(PulseWeight).toString())
											if (BigDecimal(IndexAtZeroHourInCubicMetre) < 0 && BigDecimal(IndexAtZeroHourInPulses) >= 0 && BigDecimal(PulseWeight) >= 0) {
												mensajeLecturas.put("valor", (BigDecimal(IndexAtZeroHourInPulses)*BigDecimal(PulseWeight)).toString())
											}
										}

										if ("Unit".equals(xmlReader.getAttributeName(i).toString))
											mensajeLecturas.put("unidad", xmlReader.getAttributeValue(i) )
									}
									)

								// Indicar la vesión del dispositivo de lectura.
								// Ejemplo: "MiuCybleFdc", "MiuCybleEverBluV2_1", "MiuPulseEverblu" o "MiuPulseEverBluV2".
								// Se puede descodificar a partir de la etiqueta del atributo de la posicion26.
								if ("MiuCybleFdc".equals(xmlReader.getLocalName) ||
									"MiuPulseEverblu".equals(xmlReader.getLocalName) ||
									"MiuPulseEverBluV2".equals(xmlReader.getLocalName)) {
									mensajeLecturas.put("version", xmlReader.getLocalName )
								}

								if ("MiuCybleEverBluV2_1".equals(xmlReader.getLocalName)) {
									// Indicar la vesión del dispositivo de lectura.
									// Ejemplo: "MiuCybleFdc", "MiuCybleEverBluV2_1", "MiuPulseEverblu" o "MiuPulseEverBluV2".
									// Se puede descodificar a partir de la etiqueta del atributo de la posicion26.
									mensajeLecturas.put("version", xmlReader.getLocalName )
									(0 until xmlReader.getAttributeCount).foreach(i => {
										if ("ID".equals(xmlReader.getAttributeName(i).toString)) {
											mensajeLecturas.put("id", xmlReader.getAttributeValue(i) )
											informacionAdicional.put("id", xmlReader.getAttributeValue(i))
											posicion26idDeMiuCybleEverBluV2_1 = xmlReader.getAttributeValue(i)
										}
										if ("CollectorSerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
											tipo_activo_padre = "colector"
											posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1 = xmlReader.getAttributeValue(i)
										}
										if ("SerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
											mensajeLecturas.put("numero_serie", xmlReader.getAttributeValue(i) )
											informacionAdicional.put("numero_serie", xmlReader.getAttributeValue(i))
											posicion28SerialNumberDeMiuCybleEverBluV2_1 = xmlReader.getAttributeValue(i)
										}
									}
									)
								}

								if ("MiuCybleEverBluDataItemV2_1".equals(xmlReader.getLocalName))
									(0 until xmlReader.getAttributeCount).foreach(i => {
										if ("DataDate".equals(xmlReader.getAttributeName(i).toString)) {
											mensajeLecturas.put("fecha", xmlReader.getAttributeValue(i) )
										}
									}
									)

								if ("MiuCybleEverBluDailyDataV2_1".equals(xmlReader.getLocalName))
									(0 until xmlReader.getAttributeCount).foreach(i => {
										// Si IndexAtZeroHourInCubicMetre está cumplimentado, se compone el mensaje con ese valor.
										// Si no viene cumplimentado, asignar el valor IndexAtZeroHourInPulses*PulseWeight
										if ("IndexAtZeroHourInCubicMetre".equals(xmlReader.getAttributeName(i).toString)) {
											IndexAtZeroHourInCubicMetre = xmlReader.getAttributeValue(i)
											if (mensajeLecturas.has("valor")) {
												if (!mensajeLecturas.get("valor").equals(IndexAtZeroHourInCubicMetre)) {
													mensajeLecturas.remove("valor")
													mensajeLecturas.put("valor", IndexAtZeroHourInCubicMetre)
												}
											}
											mensajeLecturas.put("valor", IndexAtZeroHourInCubicMetre)
										}
										if ("IndexAtZeroHourInPulses".equals(xmlReader.getAttributeName(i).toString)) {
											IndexAtZeroHourInPulses = xmlReader.getAttributeValue(i)
											if (BigDecimal(IndexAtZeroHourInCubicMetre) < 0 && BigDecimal(IndexAtZeroHourInPulses) >= 0 && BigDecimal(PulseWeight) >= 0) {
												mensajeLecturas.put("valor", (BigDecimal(IndexAtZeroHourInPulses)*BigDecimal(PulseWeight)).toString())
											}
										}

										informacionAdicional.put("fecha_inicio", "")
										informacionAdicional.put("fecha_fin", "")
										informacionAdicional.put("alarma_general", "")
										informacionAdicional.put("alarma_bloqueo", "")
										informacionAdicional.put("alarma_desincronizacion", "")
										informacionAdicional.put("tension_bateria", "")
										informacionAdicional.put("meses_bateria", "")
										informacionAdicional.put("intervalo_tiempo_volumen_flujo_inverso", "86400")
										informacionAdicional.put("numero_arranques", "")
										informacionAdicional.put("intervalo_tiempo_numero_arranques", "")
										informacionAdicional.put("tiempo_sin_paso_agua", "")
										informacionAdicional.put("intervalo_tiempo__sin_paso_agua", "")
										informacionAdicional.put("presion", "")
										informacionAdicional.put("temperatura", "")

										if ("DailyLeakageAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_fuga", xmlReader.getAttributeValue(i))
										}

										if ("BatteryAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_bateria", xmlReader.getAttributeValue(i))
										}

										if ("DailyBackflowAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_flujo_inverso", xmlReader.getAttributeValue(i))
										}

										if ("MonthlyBelowAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_subgasto", xmlReader.getAttributeValue(i))
										}

										if ("MonthlyAboveAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_sobregasto", xmlReader.getAttributeValue(i))
										}

										if ("MagneticTamperInformation".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_fraude_magnetico", xmlReader.getAttributeValue(i))
										}

										if ("RealTimeRemovalAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_desinstalacion", xmlReader.getAttributeValue(i))
										}

										if ("RealTimeTamperAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_manipulacion", xmlReader.getAttributeValue(i))
										}

										if ("ReversedMeterAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_contador_invertido", xmlReader.getAttributeValue(i))
										}

										if ("DailyPeakAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("alarma_pico_consumo", xmlReader.getAttributeValue(i))
										}

										if ("BackflowIndexInCubicMetre".equals(xmlReader.getAttributeName(i).toString)) {
											informacionAdicional.put("volumen_flujo_inverso", xmlReader.getAttributeValue(i))
										}

										if (xmlReader.getAttributeName(i).toString.startsWith("HourlyConsumptionInPulses"))
											mensajeLecturas.put(xmlReader.getAttributeName(i).toString.replace("HourlyConsumptionInPulses", "Pulsos"), xmlReader.getAttributeValue(i) )
									}
									)
							}
						}

					} catch {
						case e: Exception => {
							if (!contenidoArchivo.startsWith("<?xml"))
								println(e.toString)
						}
					}

					if (mensajeLecturas.has("id") &&
						mensajeLecturas.has("numero_serie") &&
						mensajeLecturas.has("unidad")) {

							var lecturasSeparadas = separaEn24lecturas(mensajeLecturas)

							jsonLecturas1XML.put("codigo_mensaje", "0001")
							jsonLecturas1XML.put("fabricante", "ITRON")
							jsonLecturas1XML.put("guardar", "true")
							jsonLecturas1XML.put("lecturas", lecturasSeparadas)
							jsonLecturas1XML.put("alarmas", informacionAdicional)

							jsonLecturas.add(jsonLecturas1XML)
							// println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturas))

							mensajeLecturas = mapeadorJackson.createObjectNode()
							informacionAdicional = mapeadorJackson.createObjectNode
					}

					if (!tipo_activo.equals("")) {
							mensajeTopologia = mapeadorJackson.createObjectNode()
							mensajeTopologia.put("fecha", fechaMensajeTopologia)
							mensajeTopologia.put("tipo_activo", tipo_activo)
							mensajeTopologia.put("tipo_activo_padre", tipo_activo_padre)

							if (tipo_activo.equals("modulo")) {
								mensajeTopologia.put("serial_number", posicion28SerialNumberDeMiuCybleEverBluV2_1)
								mensajeTopologia.put("id", posicion26idDeMiuCybleEverBluV2_1)
							} else if (tipo_activo.equals("colector")) {
								mensajeTopologia.put("serial_number", posicion16SerialNumberDeCollector)
								mensajeTopologia.put("id", "")
							}

							if (tipo_activo_padre.equals("gateway")) {
								mensajeTopologia.put("id_padre", posicion10_SerialNumberDeAccessPoint_idGateway)
							} else if (tipo_activo.equals("colector") && tipo_activo_padre.equals("colector")) {
								mensajeTopologia.put("id_padre", posicion17ParentSerialNumberDeCollector)
							} else if (tipo_activo.equals("modulo") && tipo_activo_padre.equals("colector")) {
								mensajeTopologia.put("id_padre", posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1)
							}

							jsonVectorTopologias.add(mensajeTopologia)

							jsonTopologias.put("codigo_mensaje", "0002")
							jsonTopologias.put("fabricante", "ITRON")
							jsonTopologias.put("topologias", jsonVectorTopologias)

							tipo_activo = ""
							tipo_activo_padre = "gateway"
							fechaMensajeTopologia = ""
							posicion10_SerialNumberDeAccessPoint_idGateway = ""
							posicion16SerialNumberDeCollector = ""
							posicion17ParentSerialNumberDeCollector = ""
							posicion26idDeMiuCybleEverBluV2_1 = ""
							posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1 = ""
							posicion28SerialNumberDeMiuCybleEverBluV2_1 = ""
					}

				}
			}

			case "SAPPEL" => {
				contenidoArchivo.split("\n")foreach(linea => {
					try {
						val registro = linea.split(";",-1)

						val mensajeSappel = mapeadorJackson.createObjectNode

						mensajeSappel.put("valor1_valor2_valor3", nombreArchivo.substring(0, nombreArchivo.indexOf(".")))
						mensajeSappel.put("fechalectura", registro(0))
						// mensajeSappel = mensajeSappel + ("" -> registro(1).toString)
						mensajeSappel.put("cod_fabricante", registro(2))
						mensajeSappel.put("id_tRF", registro(3))
						mensajeSappel.put("alarma", registro(4))
						mensajeSappel.put("generador_aleatorio", registro(5))
						mensajeSappel.put("intervalo_emision", registro(6))
						mensajeSappel.put("fuga", registro(7))
						mensajeSappel.put("fuga_h", registro(8))
						mensajeSappel.put("medidor_bloqueado", registro(9))
						mensajeSappel.put("estado_bateria", registro(10))
						mensajeSappel.put("retorno_agua", registro(11))
						mensajeSappel.put("siempre_subgasto", registro(12))
						mensajeSappel.put("sobre_gasto", registro(13))
						mensajeSappel.put("fraude_magnetico", registro(14))
						mensajeSappel.put("fraude_magnetico_h", registro(15))
						mensajeSappel.put("fraude_mecanico", registro(16))
						mensajeSappel.put("fraude_mecanico_h", registro(17))
						mensajeSappel.put("resolucion_sensor", registro(18))
						mensajeSappel.put("error_sensor", registro(19))
						mensajeSappel.put("intervalo_sensor", registro(20))
						mensajeSappel.put("indice", registro(21))
						mensajeSappel.put("unidad", registro(22))
						mensajeSappel.put("indice_ant", registro(23))
						mensajeSappel.put("unidad_ant", registro(24))
						mensajeSappel.put("fecha_ant", registro(25))
						mensajeSappel.put("value27", registro(26))

						// jsonDelXML.add(mensajeSappel)

					} catch {
						case ex: IndexOutOfBoundsException =>
							println("Error parseando la linea "+ linea)
					}

				})

			}

			case "CONTAZARA" => {
				var mensajeContazara = mapeadorJackson.createObjectNode
				mensajeContazara.put("id", "")

				contenidoArchivo.split("\n\t")foreach(linea => {

					if (!linea.equals("\t")) {
						try {
							if (!linea.equals("\t") && !linea.trim.equals("")) {
								try {
									val campo = linea.split(" => ")(0).replace("\t", "")
									val valor = linea.split(" => ")(1).replace("\n", "")

									// Ponemos un filtro hipotético de cuales son los campos específicos de lectura
									// Modificaremos la versión final cuando los hayamos identificado en futuras reuniones

									if (campo.equals("date")) {

										try {
											val formato: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

											val localDate = LocalDate.parse(valor, formato)
											val localDateTime = localDate.atStartOfDay
											val instant = localDateTime.toInstant(ZoneOffset.UTC)

											mensajeContazara.put("instanteTomaLectura", instant.toString)

										} catch {
											case ex: Exception => println(ex.printStackTrace())
										}

									} else if (campo.equals("bus_meters")) {

										mensajeContazara.put("cifra", valor)

									}

									if (linea.startsWith("bdaddress")) {
										// jsonDelXML.add(mensajeContazara)

										// enviaJsonAMicroservicioPrincipal(mensajeContazara.toString, variablesAguas.restEndpointLecturasPrincipal)

										mensajeContazara = mapeadorJackson.createObjectNode
										mensajeContazara.put("id", "")
									}

								} catch {
									case ex: Exception => // mensajeContazara.put(linea.split(" => ")(0).replace("\t", ""), "")
								}
							}
						} catch {
							case ex: Exception => {
								println("Error parseando el registro "+ linea)
								println(ex.printStackTrace())
							}
						}
					}
				})

			}

		}

		// println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonTopololgia))
		// new PrintWriter("/tmp/lecturas.json") { write(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturas)); close }
		// new PrintWriter("/tmp/topologia.json") { write(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonTopologias)); close }

		// enviaJsonAMicroservicioPrincipal(jsonTopologias.toString, variablesAguas.restEndpointLecturasPrincipal)
		// enviaJsonAMicroservicioPrincipal(jsonLecturas.toString, variablesAguas.restEndpointLecturasPrincipal)

		(jsonTopologias, jsonLecturas)

	}


	def parseaCamposArchivoTelelecturas(remoteFilePath: String): Unit = {
		try {
			var ftpClient : FTPClient = new FTPClient

			ftpClient.connect(variablesAguas.ftpUrl, variablesAguas.ftpPort)
			ftpClient.login(variablesAguas.ftpUser, variablesAguas.ftpPass)

			// Usamos el modo "local passive" para rebasar el firewall
			ftpClient.enterLocalPassiveMode()
			ftpClient.setKeepAlive(true)


			ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

			val inputStream: InputStream = ftpClient.retrieveFileStream(remoteFilePath)
			val contenidoArchivo : String = IOUtils.toString(inputStream, StandardCharsets.UTF_8)

			val marca : String = {
				if (remoteFilePath.contains("/ITRON/") || remoteFilePath.endsWith(".xml")) "ITRON"
				else if (remoteFilePath.contains("/CONTAZARA/")) "CONTAZARA"
				else if (remoteFilePath.contains("/IKOR/")) "IKOR"
				else if (remoteFilePath.contains("/ITRON/")) "ITRON"
				else if (remoteFilePath.contains("/SAPPEL/")) "SAPPEL"
				else if (remoteFilePath.contains("/SENSUS_ELSTER_ARSON/")) "SENSUS"
				else if (remoteFilePath.contains("/ABERING/")) "ABERING"
				else ""
			}

			val kafkaTopic : String = {
				if (remoteFilePath.contains("/ITRON/") || remoteFilePath.endsWith(".xml")) variablesAguas.topicoTelelecturasItron
				else if (remoteFilePath.contains("/CONTAZARA/")) variablesAguas.topicoTelelecturasContazara
				else if (remoteFilePath.contains("/IKOR/")) variablesAguas.topicoTelelecturasIkor
				else if (remoteFilePath.contains("/ITRON/")) variablesAguas.topicoTelelecturasItron
				else if (remoteFilePath.contains("/SAPPEL/")) variablesAguas.topicoTelelecturasSappel
				else if (remoteFilePath.contains("/SENSUS_ELSTER_ARSON/")) variablesAguas.topicoTelelecturasSensus
				else ""
			}

			val vectorDeMensajesJson = deArchivoOrigenAMensajeJson(contenidoArchivo.replace("\uFEFF<", "<"), marca, remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1))


			// Hemos enviado cada lectura al microservicio principal. Ya no tenemos que enviar un vector de json
			// enviaLecturaJsonAMicroservicioPrincipal(vectorDeMensajesJson.get(3).toString(), variablesAguas.restEndpointMsPrincipal)

			// Este es un camino alternativo: el mensaje puede ser enviado a Kafka desde el microservicio
			// o puede ser enviado directamente mediante el procedimiento insertaMensajeEnKafka.
			// Mantengo este bloque descomentado mientras estemos haciendo PoCs, pero habrá que tomar una
			// decisión en algún momento.
			// El cliente Kafka que consume los mensajes que inserta el microservicio será distinto al cliente Kafka
			// que consume los mensajes de este bucle, porque son mensajes totalmente distintos
			/*
			val iteradorSobreVectorDeMensajesJson = vectorDeMensajesJson._2.elements.next().get("lecturas").elements()
			while (iteradorSobreVectorDeMensajesJson.hasNext) {
				val mensajeJson = iteradorSobreVectorDeMensajesJson.next
				mensajeJson.get("")
				println(mensajeJson)
				insertaMensajeEnKafka(mensajeJson.toString(), variablesAguas.topicoLecturasFromMsPrincipal)
			}
			*/

			val vectorMensajesTopologia = vectorDeMensajesJson._1

			enviaJsonAMicroservicioPrincipal(vectorMensajesTopologia.toString, variablesAguas.restEndpointTopologiaPrincipal)

			val iteradorLecturas = vectorDeMensajesJson._2.elements

			// Este es un camino alternativo: el mensaje puede ser enviado a Kafka desde el microservicio
			// o puede ser enviado directamente mediante el procedimiento insertaMensajeEnKafka.
			// Mantengo este bloque descomentado mientras estemos haciendo PoCs, pero habrá que tomar una
			// decisión en algún momento.
			// El cliente Kafka que consume los mensajes que inserta el microservicio será distinto al cliente Kafka
			// que consume los mensajes de este bucle, porque son mensajes totalmente distintos
			while (iteradorLecturas.hasNext) {
				val mensajeJsonLecturas = iteradorLecturas.next

				println(mensajeJsonLecturas)
				// insertaMensajeEnKafka(mensajeJsonLecturas.toString(), kafkaTopic)
				enviaJsonAMicroservicioPrincipal(mensajeJsonLecturas.toString, variablesAguas.restEndpointLecturasPrincipal)
			}

		} catch {
			case ex: IOException =>
				println(ex.printStackTrace)
		}
	}

	// Campos Lecturas
	var codigo_mensaje, fabricante, guardar, version_, unidad_, id,
		numero_serie, fecha, valor = ""

	def transforma_json_de_ms_a_RDD_Lecturas(rddLecturasDeMs: RDD[(String, String)]) = {

		for(item <- rddLecturasDeMs.collect()) {
			println(item._2)
		}

		val sc = getSparkContext(variablesAguas.appParseJsonsFromMicroserviceSink)

		var rddLecturasInicial = sc.parallelize(Seq(Lecturas(id, numero_serie, fabricante, version, unidad, fecha, valor)))

		rddLecturasDeMs.mapPartitionsWithIndex( { (indice, parAtributoContenido) =>
			parAtributoContenido.map { kv => {
				var json = kv._2.substring(kv._2.indexOf("{"))

				if (json.trim.startsWith("</") && json.endsWith("/>") ) {
					json = json.replace("</", "<")
				}

				val mapeadorJackson: ObjectMapper = new ObjectMapper
				val elementosMensajeJson: JsonNode = mapeadorJackson.readTree(json)

				extraeCamposDeJson(elementosMensajeJson)

				//rddLecturasInicial.union(sc.parallelize(Seq(Lecturas(id, numero_serie, fabricante, version, unidad, fecha, valor))))
				Lecturas(id, numero_serie, fabricante, version, unidad, fecha, valor)
			}
			}
		})
	}

	// Campos Topologias
	var fecha_, tipo_activo, tipo_activo_padre, serial_number, id_, id_padre = ""

	def transforma_json_de_ms_a_RDD_Topologias(rddTopologiasDeMs: RDD[(String, String)]) = {

		for(item <- rddTopologiasDeMs.collect()) {
			println(item._2)
		}

		val sc = getSparkContext(variablesAguas.appParseJsonsFromMicroserviceSink)

		var rddTopologiasInicial = sc.parallelize(Seq(Topologia(fecha_, tipo_activo, tipo_activo_padre, serial_number, id_, id_padre)))

		rddTopologiasDeMs.mapPartitionsWithIndex( { (indice, parAtributoContenido) =>
			parAtributoContenido.map { kv => {
				var json = kv._2.substring(kv._2.indexOf("{"))

				if (json.trim.startsWith("</") && json.endsWith("/>") ) {
					json = json.replace("</", "<")
				}

				val mapeadorJackson: ObjectMapper = new ObjectMapper
				val elementosMensajeJson: JsonNode = mapeadorJackson.readTree(json)

				extraeCamposDeJson(elementosMensajeJson)

				// rddTopologiasInicial.union(sc.parallelize(Seq(Topologia(fecha_, tipo_activo, tipo_activo_padre, serial_number, id_, id_padre))))

				Topologia(fecha_, tipo_activo, tipo_activo_padre, serial_number, id_, id_padre)
			}
			}
		})
	}

	// Campos ITRON
	var _Name = ""
	var route_ID = ""
	var route_code = ""
	var route_message = ""


	def transforma_json_df_a_RDD_Itron(rddItron: RDD[(String, String)]) = {

		for(item <- rddItron.collect()) {
			println(item)
		}
		rddItron.mapPartitionsWithIndex( {(indice, valor) =>
			valor.map { kv => {
				var tupla = kv._2.replace("\uFEFF<", "<").replace(">", "/>").replace("//>", "/>").replace("<?xml", "<prueba").replace("?/>", "/>")

				if (tupla.trim.startsWith("</") && tupla.endsWith("/>") ) {
					tupla = tupla.replace("</", "<")
				}

				val mapeadorJackson: ObjectMapper = new ObjectMapper
				val elementosMensajeJson: JsonNode = mapeadorJackson.readTree(tupla)

				extraeCamposDeJson(elementosMensajeJson)

				LecturasItron(_Name, route_ID, route_code, route_message)
			}
			}
		})
	}

	// Campos SAPPEL
	var valor1_valor2_valor3 = ""
	var fechalectura = ""
	var cod_fabricante = ""
	var id_tRF = ""
	var alarma = ""
	var generador_aleatorio = ""
	var intervalo_emision = ""
	var fuga = ""
	var fuga_h = ""
	var medidor_bloqueado = ""
	var estado_bateria = ""
	var retorno_agua = ""
	var siempre_subgasto = ""
	var sobre_gasto = ""
	var fraude_magnetico = ""
	var fraude_magnetico_h = ""
	var fraude_mecanico = ""
	var fraude_mecanico_h = ""
	var error_sensor = ""
	var intervalo_sensor = ""
	var indice = ""
	var unidad = ""
	var indice_ant = ""
	var unidad_ant = ""
	var fecha_ant = ""
	var value27 = ""

	def transforma_json_df_a_RDD_Sappel(rddSappel: RDD[(String, String)]) = {

		for(item <- rddSappel.collect()) {
			println(item)
		}
		rddSappel.mapPartitionsWithIndex( {(indice, valor) =>
			valor.map { kv => {
				var tupla = kv._2.replace("\uFEFF<", "<").replace(">", "/>").replace("//>", "/>").replace("<?xml", "<prueba").replace("?/>", "/>")

				if (tupla.trim.startsWith("</") && tupla.endsWith("/>") ) {
					tupla = tupla.replace("</", "<")
				}

				val mapeadorJackson: ObjectMapper = new ObjectMapper
				val elementosMensajeJson: JsonNode = mapeadorJackson.readTree(tupla)

				extraeCamposDeJson(elementosMensajeJson)

				LecturasSappel(valor1_valor2_valor3, fechalectura, cod_fabricante, id_tRF, alarma, generador_aleatorio,
					intervalo_emision, fuga, fuga_h, medidor_bloqueado, estado_bateria, retorno_agua, siempre_subgasto, sobre_gasto,
					fraude_magnetico, fraude_magnetico_h, fraude_mecanico, fraude_mecanico_h, error_sensor, intervalo_sensor,
					indice.toString, unidad, indice_ant, unidad_ant, fecha_ant, value27)

			}
			}
		})
	}


	// Campos CONTAZARA
	var id_tGtw = ""
	var date = ""
	var time = ""
	var index_type_digits = ""
	var index_type_exponent = ""
	var manufacturer = ""
	var meter_model = ""
	var ser_num = ""
	var usub = ""
	var fsub = ""
	var version = ""
	var program = ""
	var _type = ""
	var status = ""
	var units = ""
	var qmax = ""
	var battery = ""
	var reserved = ""
	var bus_meters = ""
	var index = ""
	var last_index = ""
	var last_date = ""
	var last_time = ""
	var starts = ""
	var sleep_time = ""
	var c3b_time = ""
	var normal_time = ""
	var error_flags = ""
	var bdaddress = ""


	def transforma_json_df_a_RDD_Contazara(rddItron: RDD[(String, String)]) = {

		for(item <- rddItron.collect()) {
			println(item)
		}
		rddItron.mapPartitionsWithIndex( {(indice, valor) =>
			valor.map { kv => {
				var tupla = kv._2.replace("\uFEFF<", "<").replace(">", "/>").replace("//>", "/>").replace("<?xml", "<prueba").replace("?/>", "/>")

				if (tupla.trim.startsWith("</") && tupla.endsWith("/>") ) {
					tupla = tupla.replace("</", "<")
				}

				val mapeadorJackson: ObjectMapper = new ObjectMapper
				val elementosMensajeJson: JsonNode = mapeadorJackson.readTree(tupla)

				extraeCamposDeJson(elementosMensajeJson)

				LecturasContazara(id_tGtw, date, time, index_type_digits, index_type_exponent, manufacturer,
					meter_model, ser_num, usub, fsub, version, program, _type, status, units, qmax, battery, reserved,
					bus_meters, index, last_index, last_date, last_time, starts, sleep_time, c3b_time, normal_time,
					error_flags, bdaddress)
			}
			}
		})
	}

	def extraeCamposDeJson(nodoPadre: JsonNode): Any = {
		if (nodoPadre.isArray) { // Si el nodo comienza con [
			val nodos = nodoPadre.elements
			while (nodos.hasNext) {
				val nodo = nodos.next
				if (nodo.isObject || nodo.isArray)
					extraeCamposDeJson(nodo)
			}
		} else if (nodoPadre.isObject) { // Si el nodo comienza con {
			val iter = nodoPadre.fieldNames
			while (iter.hasNext) {
				val clave = iter.next
				val contenido = nodoPadre.path(clave)

				clave match {
					// Campos Lecturas
					case "codigo_mensaje" => codigo_mensaje = contenido.asText()
					case "fabricante" => fabricante = contenido.asText()
					case "guardar" => guardar = contenido.asText()
					case "version_" => version_ = contenido.asText()
					case "unidad" => unidad_ = contenido.asText()
					case "id" => id = contenido.asText()
					case "numero_serie" => numero_serie = contenido.asText()
					case "fecha" => fecha = contenido.asText()
					case "valor" => valor = contenido.asText()

					// Campos Topologias
					case "fecha" => fecha = contenido.asText()
					case "fabricante" => fabricante = contenido.asText()
					case "tipo_activo" => tipo_activo = contenido.asText()
					case "tipo_activo_padre" => tipo_activo_padre = contenido.asText()
					case "serial_number" => serial_number = contenido.asText()
					case "id" => id_ = contenido.asText()
					case "id_padre" => id_padre = contenido.asText()

					// Campos Itron
					case "Name" => _Name = contenido.asText()
					case "ID" => route_ID = contenido.asText()
					case "Code" => route_code = contenido.asText()
					case "Message" => route_message = contenido.asText()

					// Campos Sappel
					case "valor1_valor2_valor3" => valor1_valor2_valor3 = contenido.asText()
					case "fechalectura" => fechalectura = contenido.asText()
					case "cod_fabricante" => cod_fabricante = contenido.asText()
					case "id_tRF" => id_tRF = contenido.asText()
					case "alarma" => alarma = contenido.asText()
					case "generador_aleatorio" => generador_aleatorio = contenido.asText()
					case "intervalo_emision" => intervalo_emision = contenido.asText()
					case "fuga" => fuga = contenido.asText()
					case "fuga_h" => fuga_h = contenido.asText()
					case "medidor_bloqueado" => medidor_bloqueado = contenido.asText()
					case "estado_bateria" => estado_bateria = contenido.asText()
					case "retorno_agua" => retorno_agua = contenido.asText()
					case "siempre_subgasto" => siempre_subgasto = contenido.asText()
					case "sobre_gasto" => sobre_gasto = contenido.asText()
					case "fraude_magnetico" => fraude_magnetico = contenido.asText()
					case "fraude_magnetico_h" => fraude_magnetico_h = contenido.asText()
					case "fraude_mecanico" => fraude_mecanico = contenido.asText()
					case "fraude_mecanico_h" => fraude_mecanico_h = contenido.asText()
					case "error_sensor" => error_sensor = contenido.asText()
					case "intervalo_sensor" => intervalo_sensor = contenido.asText()
					case "indice" => indice = contenido.asText()
					// case "unidad" => unidad = contenido.asText()
					case "indice_ant" => indice_ant = contenido.asText()
					case "unidad_ant" => unidad_ant = contenido.asText()
					case "fecha_ant" => fecha_ant = contenido.asText()
					case "value27" => value27 = contenido.asText()

					// Campos Contazara
					case "id_tGtw" => id_tGtw = contenido.asText()
					case "date" => date = contenido.asText()
					case "time" => time = contenido.asText()
					case "index_type_digits" => index_type_digits = contenido.asText()
					case "index_type_exponent" => index_type_exponent = contenido.asText()
					case "manufacturer" => manufacturer = contenido.asText()
					case "meter_model" => meter_model = contenido.asText()
					case "ser_num" => ser_num = contenido.asText()
					case "usub" => usub = contenido.asText()
					case "fsub" => fsub = contenido.asText()
					case "version" => version = contenido.asText()
					case "program" => program = contenido.asText()
					case "_type" => _type = contenido.asText()
					case "status" => status = contenido.asText()
					case "units" => units = contenido.asText()
					case "qmax" => qmax = contenido.asText()
					case "battery" => battery = contenido.asText()
					case "reserved" => reserved = contenido.asText()
					case "bus_meters" => bus_meters = contenido.asText()
					case "index" => index = contenido.asText()
					case "last_index" => last_index = contenido.asText()
					case "last_date" => last_date = contenido.asText()
					case "last_time" => last_time = contenido.asText()
					case "starts" => starts = contenido.asText()
					case "sleep_time" => sleep_time = contenido.asText()
					case "c3b_time" => c3b_time = contenido.asText()
					case "normal_time" => normal_time = contenido.asText()
					case "error_flags" => error_flags = contenido.asText()
					case "bdaddress" => bdaddress = contenido.asText()

					case _ => println("No he encontrado la clave %s cuyo valor es %s".format(clave, contenido))
				}

				// Por si en algún momento se generan mensajes Json anidados ...
				if (contenido.isObject || contenido.isArray)
					extraeCamposDeJson(contenido)
			}
		}
	}


}
