package logging

/**
  * Created by logicalguess on 4/17/16.
  */

trait SparkLogging extends Logging with org.apache.spark.Logging {
  val LOG: Log = new Slf4jLogger(log)
}
