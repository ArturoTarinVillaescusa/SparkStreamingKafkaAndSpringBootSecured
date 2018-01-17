/**
  * Created by atarin on 8/02/17.
  */
package object clases {

  // Definimos el esquema de la lectura usando una case class.
  // Nota: Si tuviesemos que utilizar Scala 2.10 habríamos de tener en cuenta que tienen una limitación
  // de un máximo de 22 campos.

  // Hay dos tipos de archivo de SENSUS_ELSTER_ARSON, los que empiezan por L y los que empiezan por A
  // Los campos son distintos

  // Tipo "L": Date;            Type;Radio Address;                                                                       Calle;Numero;Poblacion;Channel;Value
  // Tipo "A": Date;NumContador;     Radio Address;Modelo;Alarma0;Alarma1;Alarma2;Alarma3;Alarma4;Alarma5;Alarma6;Alarma7

  case class LecturaSensus(Date: String, NumContador: String, Type: String, RadioAddress: String, Modelo: String,
                           Alarma0: String, Alarma1: String, Alarma2: String, Alarma3: String, Alarma4: String,
                           Alarma5: String, Alarma6: String, Alarma7: String, Calle: String, Numero: String,
                           Poblacion: String, Channel: String, Value: String)

  case class LecturasContazara(id_tGtw: String, date: String, time: String,
                               index_type_digits: String,
                               index_type_exponent: String,
                               manufacturer: String, meter_model: String, ser_num: String, usub: String,
                               fsub: String, version: String, program: String, _type: String, status: String,
                               units: String, qmax: String, battery: String, reserved: String, bus_meters: String,
                               index: String, last_index: String, last_date: String, last_time: String,
                               starts: String, sleep_time: String, c3b_time: String, normal_time: String,
                               error_flags: String, bdaddress: String)

  case class LecturasIkor(CH: String, CLS: String, CMD: String, EXTN2: String, HAC_TP: String, MFUNC: String, NOSMSC: String,
                          NOTLF: String, OPS: String, S_DIFF: String, S_TP: String, TP_SERV: String, TPLUS: String, VINST: String,
                          VSWTELIT: String, VSWZB: String,
                          NCONC: String, HAC: String, HAL: String, STZB: String, STLECT: String,
                          TN1: String, TN2: String, TFTP: String, FEB: String, FGPRS: String, REINT: String,
                          REINT_TOT: String, RESET_GM862: String, RSSI: String, VSWGPRS: String, IEEE: String)

  case class LecturasIkorRSSI(RSSI: String, FLAGS: String, F_H: String, C: String, P: String,
                              F: String, RAT: String, RC: String, TFTP: String)

  case class LecturasIkorIDC(IDC: String, NDISP: String, TN1: String, REBTS: String, EST_INST: String,
                             PORC_BAT: String, VBAT: String, FCONC: String, FCOMM: String, C_E: String,
                             E_C: String, PRIO: String, ORDEN: String)

  case class LecturasIkorIDD(IDD: String, TIPO: String, REBTS: String, PORC_BAT: String, VBAT: String,
                             FDISP: String, FCOMM: String, FHIGH: String, FLOW: String, D_C: String,
                             C_D: String, NLECT: String)

  // Descripción de los campos en
  // https://docs.google.com/spreadsheets/d/1m5HcmODu0njifVfnjkvKmjLllctZEvdUVMkB2BPG03U/edit#gid=1232008870
  case class LecturasSappel(valor1_valor2_valor3: String, fechalectura: String, cod_fabricante: String, id_tRF: String,
                            alarma: String, generador_aleatorio: String, intervalo_emision: String, fuga: String, fuga_h: String,
                            medidor_bloqueado: String, estado_bateria: String, retorno_agua: String, siempre_subgasto: String, sobre_gasto: String, fraude_magnetico: String,
                            fraude_magnetico_h: String, fraude_mecanico: String, fraude_mecanico_h: String, error_sensor: String, intervalo_sensor: String,
                            indice: String, unidad: String, indice_ant: String, unidad_ant: String, fecha_ant: String, value27: String)

  /*
  *
  *         Estos son los campos obtenidos desde databrics.xml. No estoy usando ese módulo porque no me permite leer
  *         el XML del dataframe que viene por el streaming, los muy ... obligan a que el XML a transformar esté
  *         en un archivo de disco (ver batch.ParseaArvolCarpetasDeLecturas.scala, método "aplanaItron")
  *
            .select($"_Name".alias("Nombre"), $"Route._ID", $"Route._Code", $"Route._Name", $"Route._Message", $"Route.RouteData.Customers",
              explode($"Route.RouteData.Mius.MiuCybleEverBluV2_1").alias("MiuCybleEverBluV2_1"),
              $"Route.RouteSettings.RouteFdcSettings", $"Route.RouteSettings.RouteFdcSettings.AccessPoint",
              $"Route.RouteSettings.RouteFdcSettings.AccessPoint.AccessPointCoordinates")
            .select($"Nombre", $"_ID", $"_Code", $"_Name", $"_Message", $"Customers",
              $"MiuCybleEverBluV2_1",
              explode($"MiuCybleEverBluV2_1.MiuCybleEverBluDataV2_1.MiuCybleEverBluDataItemV2_1").alias("MiuCybleEverBluDataItemV2_1"),
              $"RouteFdcSettings", $"AccessPoint", $"AccessPointCoordinates")
            .select($"Nombre", $"_ID", $"_Code", $"_Name", $"_Message", $"Customers",
              $"MiuCybleEverBluV2_1", $"MiuCybleEverBluDataItemV2_1",
              $"MiuCybleEverBluDataItemV2_1.MiuCybleEverBluDailyDataV2_1",
              $"MiuCybleEverBluDataItemV2_1.MiuCybleEverBluDailyDataV2_1.PulseValueUnit",
              $"RouteFdcSettings", $"AccessPoint", $"AccessPointCoordinates",
              explode($"AccessPoint.Collectors.Collector").alias("Collector"))
            .select($"Nombre", $"_ID", $"_Code", $"_Name", $"_Message", $"Customers",
              $"MiuCybleEverBluV2_1", $"MiuCybleEverBluDataItemV2_1",
              $"MiuCybleEverBluDailyDataV2_1",
              $"PulseValueUnit",
              $"RouteFdcSettings", $"AccessPoint", $"AccessPointCoordinates",
              $"Collector", $"Collector.CollectorStateInformation")
            .select($"Nombre", $"_ID", $"_Code", $"_Name", $"_Message", $"Customers",
              $"MiuCybleEverBluV2_1".getField("_CollectorSerialNumber"), $"MiuCybleEverBluV2_1".getField("_ID"),
              $"MiuCybleEverBluV2_1".getField("_SerialNumber"), $"MiuCybleEverBluV2_1".getField("_VALUE"),
              $"MiuCybleEverBluDataItemV2_1".getField("_DataDate"),
              $"PulseValueUnit".getField("_PulseWeight"),
              $"PulseValueUnit".getField("_Unit"),
              $"PulseValueUnit".getField("_VALUE"),
              $"PulseValueUnit".getField("_Value"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_BackflowIndexInCubicMetre"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_BackflowIndexInPulses"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_BatteryAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_DailyBackflowAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_DailyLeakageAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_DailyPeakAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_DetectionAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses00_01"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses01_02"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses02_03"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses03_04"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses04_05"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses05_06"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses06_07"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses07_08"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses08_09"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses09_10"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses10_11"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses11_12"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses12_13"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses13_14"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses14_15"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses15_16"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses16_17"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses17_18"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses18_19"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses19_20"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses20_21"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses21_22"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses22_23"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_HourlyConsumptionInPulses23_24"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_IndexAtZeroHourInCubicMetre"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_IndexAtZeroHourInPulses"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_MagneticTamperInformation"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_MemoryAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_MonthlyAboveAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_MonthlyBelowAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_PulseUnit"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_RFWakeUpAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_RFWakeUpAverage"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_RSSILevel"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_ReceptionRetry"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_RemovalAlarm"),
              $"MiuCybleEverBluDailyDataV2_1".getField("_TemporaryAlarm"),
              $"RouteFdcSettings".getField("_EndDataDate"),
              $"RouteFdcSettings".getField("_ExportDateTime"),
              $"RouteFdcSettings".getField("_StartDataDate"),
              $"AccessPoint" getField ("_SerialNumber"),
              $"AccessPoint" getField ("_WalkByWindowEndHour"),
              $"AccessPoint" getField ("_WalkByWindowStartHour"),
              $"AccessPointCoordinates" getField ("_Altitude"),
              $"AccessPointCoordinates" getField ("_Latitude"),
              $"AccessPointCoordinates" getField ("_Longitude"),
              $"AccessPointCoordinates" getField ("_VALUE"),
              $"Collector".getField("_ParentSerialNumber"),
              $"Collector".getField("_SerialNumber"),
              $"CollectorStateInformation".getField("_BatteryAlarm"),
              $"CollectorStateInformation".getField("_CRCErrorMicro1"),
              $"CollectorStateInformation".getField("_CRCErrorMicro2"),
              $"CollectorStateInformation".getField("_CorruptedDataMicro1"),
              $"CollectorStateInformation".getField("_CorruptedDataMicro2"),
              $"CollectorStateInformation".getField("_InformationDate"),
              $"CollectorStateInformation".getField("_State"),
              $"CollectorStateInformation".getField("_VALUE")
  * */
  case class LecturasItron(_name: String, route_ID: String, route_code: String, route_name: String)

  /*
	Esquema genérico de las lecturas que llegan del microservicio 'lecturas-ms-principal'
	a través del tópico de Kafka 'lecturas'
  */
  case class Lecturas(id : String, numero_serie : String, fabricante : String, version : String, unidad : String, fecha : String, valor : String)

  /*
	Esquema genérico de las topologias que llegan del microservicio 'lecturas-ms-principal'
	a través del tópico de Kafka 'lecturas'
  */
  case class Topologia(fecha: String, tipo_activo : String, tipo_activo_padre : String, serial_number : String, id : String, id_padre : String)

}