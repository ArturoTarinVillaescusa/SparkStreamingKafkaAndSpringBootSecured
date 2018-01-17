package lecturas.producers

import funciones.{variablesAguas, _}
import org.apache.commons.net.ftp.FTPClient

/**
  * Created by atarin on 7/04/17.
  */
object ProcesaFtpDeLecturasItron {
  def main(args: Array[String]): Unit = {
    var ftpClient : FTPClient = new FTPClient

    try {
      val inicio = System.currentTimeMillis()

      ftpClient.connect(variablesAguas.ftpUrl, variablesAguas.ftpPort)
      ftpClient.login(variablesAguas.ftpUser, variablesAguas.ftpPass)
      // use local passive mode to pass firewall
      ftpClient.enterLocalPassiveMode()
      ftpClient.setKeepAlive(true)
	  //ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

      System.out.println("Resultado de la conexión: " + ftpClient.getReplyString)

      val ftpRemoteFolder = variablesAguas.ftpRemoteFolder // + "/ITRON"

	  System.out.println("Starting to process files ...");
      procesaArchivosDeLecturaUbicadosEnRutaFTP(ftpClient, ftpRemoteFolder)

      ftpClient.logout
      ftpClient.disconnect()
      System.out.println("Disconnected")

	  val fin = System.currentTimeMillis()
	  println("Hemos tardado " + (fin - inicio)/1000*1000 + " segundos")

	} catch {
      case ex: Exception =>
        println("ProcesaFtpDeLecturasItron ::: " + ex.toString)
    }
  }
}
