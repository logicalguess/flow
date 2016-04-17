package logging

/**
  * Created by logicalguess on 4/17/16.
  */

import org.slf4j.{Logger, LoggerFactory}

trait Slf4JLogging extends Logging {

  val LOG: Log = new Slf4jLogger(LoggerFactory.getLogger(getClass.getName))
}

class Slf4jLogger(logger: Logger) extends Log {

  def name: String = logger.getName

  def debug(msg: String): Unit = logger.debug(msg)

  def error(msg: String): Unit = logger.error(msg)

  def error(msg: String, ex: Throwable): Unit = logger.error(msg, ex)

  def info(msg: String): Unit = logger.info(msg)

  def warn(msg: String): Unit = logger.warn(msg)

  def trace(msg: String): Unit = logger.trace(msg)

  def isDebugEnabled: Boolean = logger.isDebugEnabled

  def isErrorEnabled: Boolean = logger.isErrorEnabled

  def isInfoEnabled: Boolean = logger.isInfoEnabled

  def isWarnEnabled: Boolean = logger.isWarnEnabled

  def isTraceEnabled: Boolean = logger.isTraceEnabled

}

