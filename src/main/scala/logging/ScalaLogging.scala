package logging

import com.typesafe.scalalogging.{LazyLogging, Logger}

/**
  * Created by logicalguess on 4/17/16.
  */

trait ScalaLogging extends Logging with LazyLogging {
  implicit lazy val LOG: Log = new ScalaLogger(logger)
}

class ScalaLogger(logger: Logger) extends Log {

  def name: String = logger.underlying.getName

  def debug(msg: String): Unit = logger.debug(msg)

  def error(msg: String): Unit = logger.error(msg)

  def error(msg: String, ex: Throwable): Unit = logger.error(msg, ex)

  def info(msg: String): Unit = logger.info(msg)

  def warn(msg: String): Unit = logger.warn(msg)

  def trace(msg: String): Unit = logger.trace(msg)

  def isDebugEnabled: Boolean = logger.underlying.isDebugEnabled

  def isErrorEnabled: Boolean = logger.underlying.isErrorEnabled

  def isInfoEnabled: Boolean = logger.underlying.isInfoEnabled

  def isWarnEnabled: Boolean = logger.underlying.isWarnEnabled

  def isTraceEnabled: Boolean = logger.underlying.isTraceEnabled

}