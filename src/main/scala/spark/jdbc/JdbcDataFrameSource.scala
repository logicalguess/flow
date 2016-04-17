package spark.jdbc

import com.typesafe.config.ConfigFactory
import logging.ScalaLogging
import org.apache.spark.sql.{DataFrame, SQLContext}
import spark.SparkOperation
import util.Pimpers._


object JdbcDataFrameSource extends ScalaLogging {

  def apply(dataBaseName: String, tableName: String): SparkOperation[DataFrame] = SparkOperation { ctx ⇒
    val config = ConfigFactory.load()
    val connectionString = config.getString(s"spark.mysql.$dataBaseName.connectionString")
    val user = config.getString(s"spark.mysql.$dataBaseName.user")
    val password = config.getString(s"spark.mysql.$dataBaseName.password")

    Class.forName(config.getString("spark.mysql.driverClass"))
    val sqlContext = new SQLContext(ctx)

    sqlContext.read.jdbc(connectionString, tableName, Map("user" -> user, "password" -> password))
  }
}
