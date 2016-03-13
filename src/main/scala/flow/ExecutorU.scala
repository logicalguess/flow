package flow

import util.Pimpers._

import com.typesafe.scalalogging.LazyLogging
import scala.util.Try

/**
  * Created by logicalguess on 3/12/16.
  */

trait ExecutorU {
  //protected def configurer: Configurer
  def execute[A](operation: Operation[A]): Any
}

trait Executor[B] extends ExecutorU {
  def execute[A](operation: Operation[A]): B
}

trait TryExecutor extends Executor[Try[_]] with LazyLogging {
  def execute[A](operation: Operation[A]): Try[A] = {
    implicit lazy val log = logger
    Try {
      logger.debug("Before executing operation: " + operation.getClass.getName)
      operation.apply()
    }
      .withErrorLog("Error executing operation: " + operation.getClass.getName)
      .withFinally {
        logger.debug("After executing operation: " + operation.getClass.getName)
      }
  }
}