//TODO: add unit tests in this project

package spark.jdbc

import com.typesafe.config.ConfigFactory
import flow.Operation
import org.apache.spark.sql.{DataFrame, SaveMode}
import util.Logging
import util.Pimpers._

import scala.util.Try

object JdbcDataFrameExporter extends Logging {

  protected val config = ConfigFactory.load()

  def export(dataBaseName: String, tableName: String, saveMode: SaveMode)(dataFrame: DataFrame): Operation[(DataFrame, Try[Unit])] =
    Operation {
      val writeResult = Try {
        Class.forName(config.getString("spark.mysql.driverClass"))
        val connectionString = config.getString(s"spark.mysql.$dataBaseName.connectionString")
        val user = config.getString(s"spark.mysql.$dataBaseName.user")
        val password = config.getString(s"spark.mysql.$dataBaseName.password")
        dataFrame.write.mode(SaveMode.Overwrite)
          .jdbc(connectionString, tableName, Map("user" -> user, "password" -> password))
      }.withErrorLog(s"Failed to write to JDBC database '$dataBaseName', table '$tableName'")
      (dataFrame, writeResult)
    }
}
