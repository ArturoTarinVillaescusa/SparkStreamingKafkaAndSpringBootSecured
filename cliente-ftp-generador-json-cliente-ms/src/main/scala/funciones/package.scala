import java.io._
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import configuracion.VariablesCentralizadas
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.{PostMethod, StringRequestEntity}
import org.apache.commons.io.IOUtils
import org.apache.commons.net.ftp.{FTP, FTPClient}
import org.joda.time.DateTime


/**
  * Created by atarin on 8/02/17.
  *
  * Son las funciones no pertenecientes a la carga batch y Spark Streaming por lineas
  *
  *
  *
  */
package object funciones {

	val variablesAguas = VariablesCentralizadas.VariablesTelelecturas

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

					//	procesaArchivosDeLecturaUbicadosEnRutaFTP(ftpClient, ftpRemoteFolder + "/" + currentFileName)

					} else {
						val from = ftpRemoteFolder + "/" + currentFileName
						val to = ftpRemoteFolder + "/leidos/" + currentFileName
						System.out.println("Parsing " + from + "file ...");
						parseaCamposArchivoTelelecturas(from)
						ftpClient.rename(from,to)

					}
				}
			}
		}
	}
/*
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
						println("separaEn24lecturas ::: " + e.toString)
						println("El atributo 'fecha' vale " + fecha)
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
			mensajeLectura.put("id_watersupply", UUID.randomUUID().toString)

			jsonLecturasSeparadas.add(mensajeLectura)

		}
		)

		// println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturasSeparadas))
		jsonLecturasSeparadas

	}
*/

	import java.util.TimeZone

	val timeZone = TimeZone.getTimeZone("Europe/Madrid")

	//PJEL:COMPLETAR
	def calcularFechaLecturaItron(dia:String, index:Int) = {

		"%sT%02d:00:00+01:00".format(dia,index)

	}

	def calcularFechaItron(fecha:String, inicio:Boolean) = {
		val test = new DateTime()
		if(inicio) fecha+"T00:00:00+01:00"
		else fecha+"T23:59:59+01:00"


		/*val dt = new DateTime(fechaAux)
		var horaActualiza = dt
		val verano = timeZone.inDaylightTime(new Date(dt.getMillis))
		if(verano){
			horaActualiza = dt.plusHours(1)
		}


		import org.joda.time.format.ISODateTimeFormat
		val fmt = ISODateTimeFormat.dateTime

		val str = fmt.print(horaActualiza)*/

	}



      /*  Usamos el mismo parseador de xml que utiliza Databricks. Para CSV he preferido utilizar un bucle.
        Para genear el mensaje JSon utilizo Jackson, que es más versátil que la biblioteca 	que utiliza Databricks
         */
	def deArchivoOrigenAMensajeJson(contenidoArchivo: String, marca: String, nombreArchivo: String) = {

		val mapeadorJackson = new ObjectMapper
		// var jsonDelXML = mapeadorJackson.createArrayNode
		val jsonVectorTopologias = mapeadorJackson.createArrayNode

		var jsonTopologias = mapeadorJackson.createObjectNode
		jsonTopologias.put("codigo_mensaje", "0002")
		jsonTopologias.put("fabricante", "ITRON")

		val jsonLecturas = mapeadorJackson.createObjectNode
		val jsonArrayLecturas = mapeadorJackson.createArrayNode


		//var jsonLecturas1XML = mapeadorJackson.createObjectNode

		marca match {

			case "ITRON" => {

				jsonLecturas.put("codigo_mensaje", "0001")
				jsonLecturas.put("fabricante", "ITRON")
				jsonLecturas.put("guardar", "true")


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


				var StartDataDate = ""
				var EndDataDate = ""
				var fechaMensajeTopologia = ""
				var fechaMensajeLectura = ""
				var tipo_activo = ""
				var tipo_activo_padre = ""
				var posicion10_SerialNumberDeAccessPoint_idGateway = ""
				var posicion16SerialNumberDeCollector = ""
				var posicion17ParentSerialNumberDeCollector = ""
				var posicion26idDeMiuCybleEverBluV2_1 = ""
				var posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1 = ""
				var posicion28SerialNumberDeMiuCybleEverBluV2_1 = ""

//				var mensajeLecturas = mapeadorJackson.createObjectNode
//				var informacionAdicional = mapeadorJackson.createObjectNode
				var itronVersion = ""

				// Variables de PulseValueUnit
				var PulseWeight = ""
				var PulseValue = ""
				var PulseValueUnit = ""


				//MiuCybleEverBluDailyDataV2_1
				var DailyLeakageAlarm=""
				var ReceptionRetry=""
				var RFWakeUpAlarm=""
				var RFWakeUpAverage=""
				var RSSILevel=""
				var HourlyConsumptionInPulses00_01=""
				var HourlyConsumptionInPulses01_02=""
				var HourlyConsumptionInPulses02_03=""
				var HourlyConsumptionInPulses03_04=""
				var HourlyConsumptionInPulses04_05=""
				var HourlyConsumptionInPulses05_06=""
				var HourlyConsumptionInPulses06_07=""
				var HourlyConsumptionInPulses07_08=""
				var HourlyConsumptionInPulses08_09=""
				var HourlyConsumptionInPulses09_10=""
				var HourlyConsumptionInPulses10_11=""
				var HourlyConsumptionInPulses11_12=""
				var HourlyConsumptionInPulses12_13=""
				var HourlyConsumptionInPulses13_14=""
				var HourlyConsumptionInPulses14_15=""
				var HourlyConsumptionInPulses15_16=""
				var HourlyConsumptionInPulses16_17=""
				var HourlyConsumptionInPulses17_18=""
				var HourlyConsumptionInPulses18_19=""
				var HourlyConsumptionInPulses19_20=""
				var HourlyConsumptionInPulses20_21=""
				var HourlyConsumptionInPulses21_22=""
				var HourlyConsumptionInPulses22_23=""
				var HourlyConsumptionInPulses23_24=""
				var IndexAtZeroHourInPulses=""
				var BatteryAlarm=""
				var DetectionAlarm=""
				var RemovalAlarm=""
				var TemporaryAlarm=""
				var MemoryAlarm=""
				var PulseUnit=""
				var IndexAtZeroHourInCubicMetre=""
				var BackflowIndexInPulses=""
				var BackflowIndexInCubicMetre=""
				var DailyBackflowAlarm=""
				var MonthlyBelowAlarm=""
				var MonthlyAboveAlarm=""
				var DailyPeakAlarm=""
				var MagneticTamperInformation=""


				var ItronPulsosLectura = new Array[Int](24)

				// MiuPulseEverBluDailyDataV2
				var MemorizedRemovalAlarm = ""
				var MemorizedTamperAlarm = ""
				var RealTimeRemovalAlarm = ""
				var ReversedMeterAlarm = ""
				var RealTimeTamperAlarm = ""

				def resetVariablesColector(): Unit ={
					tipo_activo = ""
					tipo_activo_padre = ""
					fechaMensajeTopologia = ""
					posicion16SerialNumberDeCollector = ""
					posicion17ParentSerialNumberDeCollector = ""
					posicion26idDeMiuCybleEverBluV2_1 = ""
					posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1 = ""
					posicion28SerialNumberDeMiuCybleEverBluV2_1 = ""

				}

				def resetVariablesMiuDaily(): Unit ={
					MonthlyBelowAlarm=""
					MonthlyAboveAlarm=""
					DailyPeakAlarm=""
					MagneticTamperInformation=""
					DailyBackflowAlarm=""

					PulseWeight = ""
					PulseValue = ""
					PulseValueUnit = ""

					MemorizedRemovalAlarm = ""
					MemorizedTamperAlarm = ""
					RealTimeRemovalAlarm = ""
					ReversedMeterAlarm = ""
					RealTimeTamperAlarm = ""

					PulseUnit=""
					IndexAtZeroHourInCubicMetre=""
					BackflowIndexInPulses=""
					BackflowIndexInCubicMetre=""

					itronVersion = ""
				}

				while(xmlReader.hasNext()){

					var event = xmlReader.next()

					try {
						event match {

							//case XMLStreamConstants.CHARACTERS => if(false) println(xmlReader.getText().trim())

							case XMLStreamConstants.END_ELEMENT => {
								val XMLTag = xmlReader.getLocalName


								if(XMLTag == "Collector") {

									var mensajeTopologia = mapeadorJackson.createObjectNode()
									mensajeTopologia.put("fecha", calcularFechaItron(fechaMensajeTopologia,true))
									mensajeTopologia.put("serial_number", posicion16SerialNumberDeCollector)
									mensajeTopologia.put("id", "")
									mensajeTopologia.put("tipo_activo", tipo_activo)
									if(tipo_activo_padre == "gateway"){
										mensajeTopologia.put("id_padre", posicion10_SerialNumberDeAccessPoint_idGateway)
									} else {
										mensajeTopologia.put("id_padre", posicion17ParentSerialNumberDeCollector)
									}

									mensajeTopologia.put("tipo_activo_padre", tipo_activo_padre)

									jsonVectorTopologias.add(mensajeTopologia)
									resetVariablesColector()
								}

								if(XMLTag == "MiuCybleEverBluV2_1" || XMLTag == "MiuCybleFdc" || XMLTag == "MiuPulseEverBluV2" || XMLTag == "MiuPulseEverblu"){
									val mensajeTopologia = mapeadorJackson.createObjectNode()
									fechaMensajeTopologia = EndDataDate
									mensajeTopologia.put("fecha", calcularFechaItron(fechaMensajeTopologia,true))
									mensajeTopologia.put("serial_number", posicion28SerialNumberDeMiuCybleEverBluV2_1)
									mensajeTopologia.put("id", posicion26idDeMiuCybleEverBluV2_1)
									mensajeTopologia.put("tipo_activo", tipo_activo)
									mensajeTopologia.put("id_padre", posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1)
									mensajeTopologia.put("tipo_activo_padre", tipo_activo_padre)

									jsonVectorTopologias.add(mensajeTopologia)
									resetVariablesColector()

								}

								//MiuCybleEverBluDailyDataV2_1,MiuPulseEverbluDailyData y MiuPulseEverBluDailyDataV2 siempre tiene PulseValueUnit
								if(XMLTag == "PulseValueUnit" || XMLTag == "MiuCybleFdcDailyData") {
									val jsonLecturasSeparadas = mapeadorJackson.createArrayNode
									var valorLecturaCalculado = -1.0

									val IndexAtZeroHourInCubicMetreAux = IndexAtZeroHourInCubicMetre.toDouble
									val PulseWeightAux = {
										if(!PulseWeight.isEmpty){
											PulseWeight.toDouble
										}
										else {
											PulseUnit match  {
												case "Hectolitre" => 0.1
												case "Decalitre" => 0.01
												case "Litre" => 0.001
												case _ => 1.0

											}
										}
									}
									val IndexAtZeroHourInPulsesAux = Integer.parseInt(IndexAtZeroHourInPulses)
									if (!IndexAtZeroHourInCubicMetre.isEmpty && IndexAtZeroHourInCubicMetreAux >= 0.0) {
										valorLecturaCalculado = IndexAtZeroHourInCubicMetreAux
									} else if (PulseWeightAux > 0 && IndexAtZeroHourInPulsesAux > 0) {
										valorLecturaCalculado = (PulseWeightAux * IndexAtZeroHourInPulsesAux).toInt
									}

									var i =0
									var acumulado = 0
									for (i <- 0 to 23)
									{

										val mensajeLecturas = mapeadorJackson.createObjectNode()
										var uuid = UUID.randomUUID.toString
										mensajeLecturas.put("id_watersupply", uuid)
										mensajeLecturas.put("version", itronVersion)
										mensajeLecturas.put("id", posicion26idDeMiuCybleEverBluV2_1)
										mensajeLecturas.put("numero_serie", posicion28SerialNumberDeMiuCybleEverBluV2_1)
										mensajeLecturas.put("fecha", calcularFechaLecturaItron(fechaMensajeLectura, i))
										val valorLecturaAux = valorLecturaCalculado + acumulado*PulseWeightAux
										mensajeLecturas.put("valor", valorLecturaAux)
										mensajeLecturas.put("unidad", PulseUnit match  {
											case "Hectolitre" => "hl"
											case "Decalitre" => "dal"
											case "Litre" => "l"
											case _ => "l"

										})
										mensajeLecturas.put("precision", if(!PulseValueUnit.isEmpty) PulseValueUnit else "m3")

										jsonLecturasSeparadas.add(mensajeLecturas)
										acumulado += ItronPulsosLectura(i)


									}

									val informacionAdicional = mapeadorJackson.createObjectNode()

									informacionAdicional.put("fecha_inicio", fechaMensajeLectura)
									informacionAdicional.put("fecha_fin", fechaMensajeLectura)
									informacionAdicional.put("alarma_general", "")
									informacionAdicional.put("alarma_bloqueo", "")
									informacionAdicional.put("alarma_desincronizacion", "")
									informacionAdicional.put("tension_bateria", "")
									informacionAdicional.put("meses_bateria", "")
									informacionAdicional.put("intervalo_tiempo_volumen_flujo_inverso", "86400")
									informacionAdicional.put("numero_arranques", "")
									informacionAdicional.put("intervalo_tiempo_numero_arranques", "")
									informacionAdicional.put("tiempo_sin_paso_agua", "")
									informacionAdicional.put("intervalo_tiempo_sin_paso_agua", "")
									informacionAdicional.put("presion", "")
									informacionAdicional.put("temperatura", "")
									informacionAdicional.put("alarma_fuga", DailyLeakageAlarm)
									informacionAdicional.put("alarma_bateria", BatteryAlarm)
									informacionAdicional.put("alarma_flujo_inverso", DailyBackflowAlarm)
									informacionAdicional.put("alarma_subgasto", MonthlyBelowAlarm)
									informacionAdicional.put("alarma_sobregasto", MonthlyAboveAlarm)
									informacionAdicional.put("alarma_fraude_magnetico", MagneticTamperInformation)
									informacionAdicional.put("alarma_desinstalacion", RealTimeRemovalAlarm)
									informacionAdicional.put("alarma_manipulacion", RealTimeTamperAlarm)
									informacionAdicional.put("alarma_contador_invertido", ReversedMeterAlarm)
									informacionAdicional.put("alarma_pico_consumo", DailyPeakAlarm)
									informacionAdicional.put("volumen_flujo_inverso", BackflowIndexInCubicMetre)


									val mensajeLecturasEInfo = mapeadorJackson.createObjectNode()

									mensajeLecturasEInfo.put("Datolecturas",jsonLecturasSeparadas)
									mensajeLecturasEInfo.put("informacionAdicional",informacionAdicional)

									jsonArrayLecturas.add(mensajeLecturasEInfo)






									/*										PJEL: Mover todo esto al cierre del tag MiuCybleEverBluDailyDataV2_1
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
                                                          */


								}

								//Finalizar los json
								if(XMLTag == "Mius") {
									jsonLecturas.put("lecturas",jsonArrayLecturas)
									jsonTopologias.put("topologia",jsonVectorTopologias)
								}
							}


							case XMLStreamConstants.START_ELEMENT => {
								val XMLTag = xmlReader.getLocalName

								if ("AccessPoint".equals(XMLTag))
									(0 until xmlReader.getAttributeCount).foreach(i => {

											if ("SerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
												// mensajeTopologia.put("gatewaySN", xmlReader.getAttributeValue(i) )
												posicion10_SerialNumberDeAccessPoint_idGateway = xmlReader.getAttributeValue(i)
												tipo_activo= "gateway"
												val mensajeTopologia = mapeadorJackson.createObjectNode()
												fechaMensajeTopologia = EndDataDate
												mensajeTopologia.put("fecha", calcularFechaItron(fechaMensajeTopologia,true))
												mensajeTopologia.put("serial_number", posicion10_SerialNumberDeAccessPoint_idGateway)
												mensajeTopologia.put("id", "")
												mensajeTopologia.put("tipo_activo", tipo_activo)
												mensajeTopologia.put("id_padre", "")
												mensajeTopologia.put("tipo_activo_padre", "")

												jsonVectorTopologias.add(mensajeTopologia)
												resetVariablesColector()
											}
										}
										)

								if ("RouteFdcSettings".equals(XMLTag))
									(0 until xmlReader.getAttributeCount).foreach(i => {

										if ("StartDataDate" == xmlReader.getAttributeName(i).toString) {
											StartDataDate = xmlReader.getAttributeValue(i)
										}

										if ("EndDataDate" == xmlReader.getAttributeName(i).toString) {
											EndDataDate = xmlReader.getAttributeValue(i)
										}
									}
									)

								if (XMLTag == "Collector") {
									tipo_activo = "colector"
									var tienePadre = false
									(0 until xmlReader.getAttributeCount).foreach(i => {

										if ("SerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
											//tipo_activo = "colector"
											posicion16SerialNumberDeCollector = xmlReader.getAttributeValue(i)
										}
										if ("ParentSerialNumber".equals(xmlReader.getAttributeName(i).toString)) {
											//tipo_activo = "modulo"
											tipo_activo_padre = "colector"
											posicion17ParentSerialNumberDeCollector = xmlReader.getAttributeValue(i)
											tienePadre = true
										}

									}
									)
									if(!tienePadre){
										tipo_activo_padre = "gateway"
										posicion17ParentSerialNumberDeCollector = posicion10_SerialNumberDeAccessPoint_idGateway

									}
								}

							 	if (XMLTag == "CollectorStateInformation") {
									(0 until xmlReader.getAttributeCount).foreach(i => {
										if (xmlReader.getAttributeName(i).toString == "InformationDate" ) {
											fechaMensajeTopologia = xmlReader.getAttributeValue(i)
										}

									}
									)

								}


								if (XMLTag == "PulseValueUnit")
									(0 until xmlReader.getAttributeCount).foreach(i => {

										if ("Value" == xmlReader.getAttributeName(i).toString) {
											PulseValue = xmlReader.getAttributeValue(i)

										}
										if ("PulseWeight" == xmlReader.getAttributeName(i).toString) {
											PulseWeight = xmlReader.getAttributeValue(i)

											/* PJEL : Revisar esto
											mensajeLecturas.put("peso", BigDecimal(PulseWeight).toString())
											if (BigDecimal(IndexAtZeroHourInCubicMetre) < 0 && BigDecimal(IndexAtZeroHourInPulses) >= 0 && BigDecimal(PulseWeight) >= 0) {
												mensajeLecturas.put("valor", (BigDecimal(IndexAtZeroHourInPulses)*BigDecimal(PulseWeight)).toString())
											}*/
										}

										if ("Unit" == xmlReader.getAttributeName(i).toString){
											PulseValueUnit = xmlReader.getAttributeValue(i)

											//mensajeLecturas.put("unidad", xmlReader.getAttributeValue(i) )

										}

									}
									)

								// Indicar la vesión del dispositivo de lectura.
								// Ejemplo: "MiuCybleFdc", "MiuCybleEverBluV2_1", "MiuPulseEverblu" o "MiuPulseEverBluV2".
								// Se puede descodificar a partir de la etiqueta del atributo de la posicion26.
								/*if (XMLTag == "MiuCybleFdc" ||
									XMLTag == "MiuPulseEverblu" ||
									XMLTag == "MiuPulseEverBluV2" ) {
									//mensajeLecturas.put("version", XMLTag )
								}*/

								if (XMLTag == "MiuCybleEverBluV2_1" || XMLTag == "MiuCybleFdc" || XMLTag == "MiuPulseEverBluV2" || XMLTag ==  "MiuPulseEverblu") {

									tipo_activo = "modulo"
									itronVersion = XMLTag

									(0 until xmlReader.getAttributeCount).foreach(i => {

										if ("ID" == xmlReader.getAttributeName(i).toString) {

											posicion26idDeMiuCybleEverBluV2_1 = xmlReader.getAttributeValue(i)
										}
										if ("CollectorSerialNumber" == xmlReader.getAttributeName(i).toString) {
											//tipo_activo = "modulo"
											tipo_activo_padre = "colector"
											posicion27CollectorSerialNumberDeMiuCybleEverBluV2_1 = xmlReader.getAttributeValue(i)

										}
										if ("SerialNumber" == xmlReader.getAttributeName(i).toString) {

											posicion28SerialNumberDeMiuCybleEverBluV2_1 = xmlReader.getAttributeValue(i)
										}
									}
									)






/*

									//PJEL: RevisaR  al rellenar infor adicional y lectura
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
									)*/
								}

								if (XMLTag == "MiuCybleEverBluDataItemV2_1" || XMLTag == "MiuCybleFdcDataItem" || XMLTag == "MiuPulseEverBluDataItemV2" || XMLTag == "MiuPulseEverbluDataItem")
									(0 until xmlReader.getAttributeCount).foreach(i => {
										if (xmlReader.getAttributeName(i).toString == "DataDate") {
											fechaMensajeLectura = xmlReader.getAttributeValue(i)
										}
									}
									)

								//PJEL QUITAR ESTE IF
								/*if (XMLTag == "MiuPulseEverBluDailyDataV2" )
									(0 until xmlReader.getAttributeCount).foreach(i => {
										/* PJEL: Revisar al rellenar info adicional
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
										informacionAdicional.put("temperatura", "")*/

										if ("DailyLeakageAlarm" == xmlReader.getAttributeName(i).toString) {
											//informacionAdicional.put("alarma_fuga", xmlReader.getAttributeValue(i))
											DailyLeakageAlarm = xmlReader.getAttributeValue(i)
										}

										if ("BatteryAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											//informacionAdicional.put("alarma_bateria", xmlReader.getAttributeValue(i))
											BatteryAlarm = xmlReader.getAttributeValue(i)
										}

										if ("DailyBackflowAlarm".equals(xmlReader.getAttributeName(i).toString)) {
											//informacionAdicional.put("alarma_flujo_inverso", xmlReader.getAttributeValue(i))
											DailyBackflowAlarm = xmlReader.getAttributeValue(i)
										}

										if ("MonthlyBelowAlarm" == xmlReader.getAttributeName(i).toString) {
											//informacionAdicional.put("alarma_subgasto", xmlReader.getAttributeValue(i))
											MonthlyBelowAlarm = xmlReader.getAttributeValue(i)
										}

										if ("MonthlyAboveAlarm" == xmlReader.getAttributeName(i)) {
											//informacionAdicional.put("alarma_sobregasto", xmlReader.getAttributeValue(i))
											MonthlyAboveAlarm = xmlReader.getAttributeValue(i)
										}

										if ("MagneticTamperInformation" == xmlReader.getAttributeName(i)) {
											//informacionAdicional.put("alarma_fraude_magnetico", xmlReader.getAttributeValue(i))
											MagneticTamperInformation = xmlReader.getAttributeValue(i)
										}


										if ("DailyPeakAlarm".equals(xmlReader.getAttributeName(i))) {
											//informacionAdicional.put("alarma_pico_consumo", xmlReader.getAttributeValue(i))
											DailyPeakAlarm = xmlReader.getAttributeValue(i)
										}

										if ("BackflowIndexInCubicMetre".equals(xmlReader.getAttributeName(i))) {
											//informacionAdicional.put("volumen_flujo_inverso", xmlReader.getAttributeValue(i))
											BackflowIndexInCubicMetre = xmlReader.getAttributeValue(i)
										}

										if ("DetectionAlarm".equals(xmlReader.getAttributeName(i))) {
											//informacionAdicional.put("detection_alarm", xmlReader.getAttributeValue(i))
											DetectionAlarm = xmlReader.getAttributeValue(i)
										}


										if ("RemovalAlarm".equals(xmlReader.getAttributeName(i))) {
											//informacionAdicional.put("removal_alarm", xmlReader.getAttributeValue(i))
											RemovalAlarm = xmlReader.getAttributeValue(i)
										}

										if ("TemporaryAlarm".equals(xmlReader.getAttributeName(i))) {
											//informacionAdicional.put("temporary_alarm", xmlReader.getAttributeValue(i))
											TemporaryAlarm = xmlReader.getAttributeValue(i)
										}

										if ("MemoryAlarm" == xmlReader.getAttributeName(i)) {
											//informacionAdicional.put("memory_alarm", xmlReader.getAttributeValue(i))
											MemoryAlarm = xmlReader.getAttributeValue(i)
										}


										if ("MemorizedRemovalAlarm" == xmlReader.getAttributeName(i)) {

											MemorizedRemovalAlarm = xmlReader.getAttributeValue(i)
										}

										if ("MemorizedTamperAlarm" == xmlReader.getAttributeName(i)) {

											MemorizedTamperAlarm = xmlReader.getAttributeValue(i)
										}


										if ("RealTimeRemovalAlarm" == xmlReader.getAttributeName(i)) {

											RealTimeRemovalAlarm = xmlReader.getAttributeValue(i)
										}

										if ("ReversedMeterAlarm".equals(xmlReader.getAttributeName(i))) {
											ReversedMeterAlarm = xmlReader.getAttributeValue(i)
										}

										if ("RealTimeTamperAlarm" == xmlReader.getAttributeName(i)) {

											RealTimeTamperAlarm = xmlReader.getAttributeValue(i)
										}

									})*/

								if (XMLTag == "MiuCybleEverBluDailyDataV2_1" || XMLTag == "MiuCybleFdcDailyData" || XMLTag == "MiuPulseEverBluDailyDataV2" || XMLTag ==  "MiuPulseEverbluDailyData")
									(0 until xmlReader.getAttributeCount).foreach(i => {
										val attr = xmlReader.getAttributeName(i).toString
										if ("DailyLeakageAlarm" == attr) {
											DailyLeakageAlarm = xmlReader.getAttributeValue(i)
										}
										if ("ReceptionRetry" == attr) {
											ReceptionRetry = xmlReader.getAttributeValue(i)
										}
										if ("RFWakeUpAlarm" == attr) {
											RFWakeUpAlarm = xmlReader.getAttributeValue(i)
										}
										if ("RFWakeUpAverage" == attr) {
											RFWakeUpAverage = xmlReader.getAttributeValue(i)
										}
										if ("RSSILevel" == attr) {
											RSSILevel = xmlReader.getAttributeValue(i)
										}
										if ("HourlyConsumptionInPulses00_01" == attr) {
											HourlyConsumptionInPulses00_01 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(0) = HourlyConsumptionInPulses00_01.toInt
										}
										if ("HourlyConsumptionInPulses01_02" == attr) {
											HourlyConsumptionInPulses01_02 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(1) = HourlyConsumptionInPulses01_02.toInt
										}

										if ("HourlyConsumptionInPulses02_03" == attr) {
											HourlyConsumptionInPulses02_03 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(2) = HourlyConsumptionInPulses02_03.toInt
										}
										if ("HourlyConsumptionInPulses03_04" == attr) {
											HourlyConsumptionInPulses03_04 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(3) = HourlyConsumptionInPulses03_04.toInt
										}
										if ("HourlyConsumptionInPulses04_05" == attr) {
											HourlyConsumptionInPulses04_05 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(4) = HourlyConsumptionInPulses04_05.toInt
										}
										if ("HourlyConsumptionInPulses05_06" == attr) {
											HourlyConsumptionInPulses05_06 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(5) = HourlyConsumptionInPulses05_06.toInt
										}
										if ("HourlyConsumptionInPulses06_07" == attr) {
											HourlyConsumptionInPulses06_07 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(6) = HourlyConsumptionInPulses06_07.toInt
										}
										if ("HourlyConsumptionInPulses07_08" == attr) {
											HourlyConsumptionInPulses07_08 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(7) = HourlyConsumptionInPulses07_08.toInt
										}
										if ("HourlyConsumptionInPulses08_09" == attr) {
											HourlyConsumptionInPulses08_09 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(8) = HourlyConsumptionInPulses08_09.toInt
										}
										if ("HourlyConsumptionInPulses09_10" == attr) {
											HourlyConsumptionInPulses09_10 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(9) = HourlyConsumptionInPulses09_10.toInt
										}
										if ("HourlyConsumptionInPulses10_11" == attr) {
											HourlyConsumptionInPulses10_11 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(10) = HourlyConsumptionInPulses10_11.toInt
										}
										if ("HourlyConsumptionInPulses11_12" == attr) {
											HourlyConsumptionInPulses11_12 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(11) = HourlyConsumptionInPulses11_12.toInt
										}
										if ("HourlyConsumptionInPulses12_13" == attr) {
											HourlyConsumptionInPulses12_13 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(12) = HourlyConsumptionInPulses12_13.toInt
										}
										if ("HourlyConsumptionInPulses13_14" == attr) {
											HourlyConsumptionInPulses13_14 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(13) = HourlyConsumptionInPulses13_14.toInt
										}
										if ("HourlyConsumptionInPulses14_15" == attr) {
											HourlyConsumptionInPulses14_15 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(14) = HourlyConsumptionInPulses14_15.toInt
										}
										if ("HourlyConsumptionInPulses15_16" == attr) {
											HourlyConsumptionInPulses15_16 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(15) = HourlyConsumptionInPulses15_16.toInt
										}
										if ("HourlyConsumptionInPulses16_17" == attr) {
											HourlyConsumptionInPulses16_17 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(16) = HourlyConsumptionInPulses16_17.toInt
										}
										if ("HourlyConsumptionInPulses17_18" == attr) {
											HourlyConsumptionInPulses17_18 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(17) = HourlyConsumptionInPulses17_18.toInt
										}
										if ("HourlyConsumptionInPulses18_19" == attr) {
											HourlyConsumptionInPulses18_19 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(18) = HourlyConsumptionInPulses18_19.toInt
										}
										if ("HourlyConsumptionInPulses19_20" == attr) {
											HourlyConsumptionInPulses19_20 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(19) = HourlyConsumptionInPulses19_20.toInt
										}
										if ("HourlyConsumptionInPulses20_21" == attr) {
											HourlyConsumptionInPulses20_21 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(20) = HourlyConsumptionInPulses20_21.toInt
										}
										if ("HourlyConsumptionInPulses21_22" == attr) {
											HourlyConsumptionInPulses21_22 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(21) = HourlyConsumptionInPulses21_22.toInt
										}
										if ("HourlyConsumptionInPulses22_23" == attr) {
											HourlyConsumptionInPulses22_23 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(22) = HourlyConsumptionInPulses22_23.toInt
										}
										if ("HourlyConsumptionInPulses23_24" == attr) {
											HourlyConsumptionInPulses23_24 = xmlReader.getAttributeValue(i)
											ItronPulsosLectura(23) = HourlyConsumptionInPulses23_24.toInt
										}
										if ("IndexAtZeroHourInPulses" == attr) {
											IndexAtZeroHourInPulses = xmlReader.getAttributeValue(i)
										}
										if ("BatteryAlarm" == attr) {
											BatteryAlarm = xmlReader.getAttributeValue(i)
										}
										if ("DetectionAlarm" == attr) {
											DetectionAlarm = xmlReader.getAttributeValue(i)
										}
										if ("RemovalAlarm" == attr) {
											RemovalAlarm = xmlReader.getAttributeValue(i)
										}
										if ("TemporaryAlarm" == attr) {
											TemporaryAlarm = xmlReader.getAttributeValue(i)
										}
										if ("MemoryAlarm" == attr) {
											MemoryAlarm = xmlReader.getAttributeValue(i)
										}
										if ("PulseUnit" == attr) {
											PulseUnit = xmlReader.getAttributeValue(i)
										}
										if ("IndexAtZeroHourInCubicMetre" == attr) {
											IndexAtZeroHourInCubicMetre = xmlReader.getAttributeValue(i)
										}
										if ("BackflowIndexInPulses" == attr) {
											BackflowIndexInPulses = xmlReader.getAttributeValue(i)
										}
										if ("BackflowIndexInCubicMetre" == attr) {
											BackflowIndexInCubicMetre = xmlReader.getAttributeValue(i)
										}
										if ("DailyBackflowAlarm" == attr) {
											DailyBackflowAlarm = xmlReader.getAttributeValue(i)
										}
										if ("MonthlyBelowAlarm" == attr) {
											MonthlyBelowAlarm = xmlReader.getAttributeValue(i)
										}
										if ("MonthlyAboveAlarm" == attr) {
											MonthlyAboveAlarm = xmlReader.getAttributeValue(i)
										}
										if ("DailyPeakAlarm" == attr) {
											DailyPeakAlarm = xmlReader.getAttributeValue(i)
										}
										if ("MagneticTamperInformation" == attr) {
											MagneticTamperInformation = xmlReader.getAttributeValue(i)
										}

										if ("MemorizedRemovalAlarm" == attr) {
											MemorizedRemovalAlarm = xmlReader.getAttributeValue(i)
										}
										if ("MemorizedTamperAlarm" == attr) {
											MemorizedTamperAlarm = xmlReader.getAttributeValue(i)
										}
										if ("RealTimeRemovalAlarm" == attr) {
											RealTimeRemovalAlarm = xmlReader.getAttributeValue(i)
										}
										if ("RealTimeTamperAlarm" == attr) {
											RealTimeTamperAlarm = xmlReader.getAttributeValue(i)
										}
										if ("DailyBackflowAlarm" == attr) {
											DailyBackflowAlarm = xmlReader.getAttributeValue(i)
										}
										if ("ReversedMeterAlarm" == attr) {
											ReversedMeterAlarm = xmlReader.getAttributeValue(i)
										}
									}

									)
									//PJEL: Esto aqui no puede ir
									/*	informacionAdicional.put("alarma_desinstalacion", "false")
									informacionAdicional.put("alarma_manipulacion", "false")
									informacionAdicional.put("alarma_contador_invertido", "false")*/
							}

							case _ => ""
						}

					} catch {
						case e: Exception => {
							if (!contenidoArchivo.startsWith("<?xml")) {
								println("deArchivoOrigenAMensajeJson ::: " + e.toString)
							//	println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(mensajeLecturas))
							}
						}
					}

					//PJEL: Revisar
//					if (mensajeLecturas.has("id") &&
//						mensajeLecturas.has("numero_serie") &&
//						mensajeLecturas.has("unidad")) {
//
//							var lecturasSeparadas = separaEn24lecturas(mensajeLecturas)
//
//							jsonLecturas1XML.put("codigo_mensaje", "0001")
//							jsonLecturas1XML.put("fabricante", "ITRON")
//							jsonLecturas1XML.put("guardar", "true")
//							jsonLecturas1XML.put("lecturas", lecturasSeparadas)
//							jsonLecturas1XML.put("alarmas", informacionAdicional)
//
//							jsonLecturas.add(jsonLecturas1XML)
//							// println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturas))
//
//							mensajeLecturas = mapeadorJackson.createObjectNode()
//							informacionAdicional = mapeadorJackson.createObjectNode
//					}



				}
			}
/*

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

			}*/
/*
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
*/
		}

		println("lecturas Json that we are going to send to the Microservice ......")
		println(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturas))
		// println(nombreArchivo)
		// new PrintWriter("/tmp/lecturas.json") { write(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonLecturas)); close }
		// new PrintWriter("/tmp/topologia.json") { write(mapeadorJackson.writerWithDefaultPrettyPrinter.writeValueAsString(jsonTopologias)); close }

	//	enviaJsonAMicroservicioPrincipal(jsonTopologias.toString, variablesAguas.restEndpointLecturasPrincipal)
	//	enviaJsonAMicroservicioPrincipal(jsonLecturas.toString, variablesAguas.restEndpointLecturasPrincipal)

		(jsonTopologias, jsonLecturas)

	}


	def parseaCamposArchivoTelelecturas( remoteFilePath: String): Unit = {


		try {
			var ftpClient : FTPClient = new FTPClient

			ftpClient.connect(variablesAguas.ftpUrl, variablesAguas.ftpPort)
			ftpClient.login(variablesAguas.ftpUser, variablesAguas.ftpPass)

			// Usamos el modo "local passive" para rebasar el firewall
			ftpClient.enterLocalPassiveMode()
			ftpClient.setKeepAlive(true)


			ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

			val inputStream: InputStream = ftpClient.retrieveFileStream(remoteFilePath)
			if(Option(inputStream).isDefined){

			System.out.println("Converting inputStream to String ...");

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

		/*	val kafkaTopic : String = {
				if (remoteFilePath.contains("/ITRON/") || remoteFilePath.endsWith(".xml")) variablesAguas.topicoTelelecturasItron
				else if (remoteFilePath.contains("/CONTAZARA/")) variablesAguas.topicoTelelecturasContazara
				else if (remoteFilePath.contains("/IKOR/")) variablesAguas.topicoTelelecturasIkor
				else if (remoteFilePath.contains("/ITRON/")) variablesAguas.topicoTelelecturasItron
				else if (remoteFilePath.contains("/SAPPEL/")) variablesAguas.topicoTelelecturasSappel
				else if (remoteFilePath.contains("/SENSUS_ELSTER_ARSON/")) variablesAguas.topicoTelelecturasSensus
				else ""
			}*/

			System.out.println("Converting input XML string to Json...");

			val vectorDeMensajesJson = deArchivoOrigenAMensajeJson(contenidoArchivo.replace("\uFEFF<", "<"), marca, remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1))
			println(s"vector de  mensajes $vectorDeMensajesJson")

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

			val MensajeLecturas = vectorDeMensajesJson._2
			println("Sending lecturas Json to lecturas Microservice ......")
			enviaJsonAMicroservicioPrincipal(MensajeLecturas.toString, variablesAguas.restEndpointLecturasPrincipal)

			val MensajeTopologia = vectorDeMensajesJson._1

			println("Sending topologias Json to topologias Microservice ......")

			enviaJsonAMicroservicioPrincipal(MensajeTopologia.toString, variablesAguas.restEndpointTopologiaPrincipal)

			println("Messages topologias and lecturas jsons sent to their respective microservice endpoints ......")

			ftpClient.logout
			ftpClient.disconnect()

			//PJEL: Esto no es lo que hay que hacer
			// Este es un camino alternativo: el mensaje puede ser enviado a Kafka desde el microservicio
			// o puede ser enviado directamente mediante el procedimiento insertaMensajeEnKafka.
			// Mantengo este bloque descomentado mientras estemos haciendo PoCs, pero habrá que tomar una
			// decisión en algún momento.
			// El cliente Kafka que consume los mensajes que inserta el microservicio será distinto al cliente Kafka
			// que consume los mensajes de este bucle, porque son mensajes totalmente distintos
			/*while (iteradorLecturas.hasNext) {
				val mensajeJsonLecturas = iteradorLecturas.next

				println(mensajeJsonLecturas)
				// insertaMensajeEnKafka(mensajeJsonLecturas.toString(), kafkaTopic)
				enviaJsonAMicroservicioPrincipal(mensajeJsonLecturas.toString, variablesAguas.restEndpointLecturasPrincipal)
			}*/
			} else println("inputStream is not defined")
		} catch {
			case ex: Exception =>
				println("parseaCamposArchivoTelelecturas ::: " + ex.toString)
				ex.printStackTrace()
				println(variablesAguas.restEndpointLecturasPrincipal)
		}


	}

	// Campos Lecturas
	var codigo_mensaje, fabricante, guardar, version_, unidad_, id,
		numero_serie, fecha, valor = ""


	// Campos Topologias
	var fecha_, tipo_activo, tipo_activo_padre, serial_number, id_, id_padre = ""


	// Campos ITRON
	var _Name = ""
	var route_ID = ""
	var route_code = ""
	var route_message = ""



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
