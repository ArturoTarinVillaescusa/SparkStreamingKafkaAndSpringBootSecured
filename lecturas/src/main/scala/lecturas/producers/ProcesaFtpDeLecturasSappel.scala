package lecturas.producers

import java.io.IOException

import funcionestodojunto.{variablesAguas, _}
import org.apache.commons.net.ftp.FTPClient

/**
  * Created by atarin on 7/04/17.
  */
object ProcesaFtpDeLecturasSappel {
  def main(args: Array[String]): Unit = {
    var ftpClient : FTPClient = new FTPClient

    try {
      ftpClient.connect(variablesAguas.ftpUrl, variablesAguas.ftpPort)
      ftpClient.login(variablesAguas.ftpUser, variablesAguas.ftpPass)
      // use local passive mode to pass firewall
      ftpClient.enterLocalPassiveMode()
      ftpClient.setKeepAlive(true)

      System.out.println("Resultado de la conexión: " + ftpClient.getReplyString)

      val ftpRemoteFolder = variablesAguas.ftpRemoteFolder + "/SAPPEL"

      procesaArchivosDeLecturaUbicadosEnRutaFTP(ftpClient, ftpRemoteFolder)

      ftpClient.logout
      ftpClient.disconnect()
      System.out.println("Disconnected")
    } catch {
      case ex: IOException =>
        ex.printStackTrace()
    }
  }
}
