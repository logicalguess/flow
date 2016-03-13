package spark

import com.typesafe.scalalogging.LazyLogging
import flow.{Executor, Transformer}
import org.apache.spark.SparkContext
import util.Pimpers._

import scala.util.Try

/**
  * Created by logicalguess on 3/12/16.
  */

// A SparkExecutor creates a spark context on the fly and releases it
trait SparkExecutor extends Executor[Try[_]] with LazyLogging {
  def execute[A](operation: Transformer[SparkContext, A]): Try[A] = {
    //logger.debug(s"SparkExecutor configuration:\n$configurer")
    //configurer { cfg ⇒
      val sparkContext = new SparkContext() //TODO
      implicit lazy val log = logger
      Try {
        logger.debug("Executing operation: " + operation.getClass.getName)
        operation(sparkContext).apply()
      }.withErrorLog("Error executing operation")
        .withFinally {
          sparkContext.stop()
        }
    //}
  }
}

// A LongLivedExecutor creates a spark context and keeps it for as long as it lives
// The StopContext must be manually called (just like in Spark)
trait LongLivedExecutor extends Executor[Try[_]] with LazyLogging {

  lazy val sparkContext: SparkContext = new SparkContext() //TODO configurer { cfg ⇒ new SparkContext(cfg) }

  def execute[A](operation: Transformer[SparkContext, A]): Try[A] = {
    implicit lazy val log = logger
    Try {
      logger.debug("Executing operation: " + operation.getClass.getName)
      operation(sparkContext).apply()
    }.withErrorLog("Error executing operation")
  }

  def stopContext(): Unit = sparkContext.stop()

}

