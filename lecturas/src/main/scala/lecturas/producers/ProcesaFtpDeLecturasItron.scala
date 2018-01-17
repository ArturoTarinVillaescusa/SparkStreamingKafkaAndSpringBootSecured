package lecturas.producers

import java.io.IOException

import org.apache.commons.net.ftp.FTPClient
import funcionestodojunto._

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

      System.out.println("Resultado de la conexión: " + ftpClient.getReplyString)

      val ftpRemoteFolder = variablesAguas.ftpRemoteFolder // + "/ITRON"

      procesaArchivosDeLecturaUbicadosEnRutaFTP(ftpClient, ftpRemoteFolder)

      ftpClient.logout
      ftpClient.disconnect()
      System.out.println("Disconnected")

	  val fin = System.currentTimeMillis()
	  println("Hemos tardado " + (fin - inicio)/1000*1000 + " segundos")

	} catch {
      case ex: IOException =>
        ex.printStackTrace()
    }
  }
}
