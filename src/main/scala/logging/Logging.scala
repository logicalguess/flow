package logging

/**
  * Created by logicalguess on 4/17/16.
  */
trait Logging {
  val LOG: Log
}

trait Log {

  def name: String

  def debug(msg: String): Unit

  def error(msg: String): Unit

  def error(msg: String, ex: Throwable): Unit

  def info(msg: String): Unit

  def warn(msg: String): Unit

  def trace(msg: String): Unit

  def isDebugEnabled: Boolean

  def isErrorEnabled: Boolean

  def isInfoEnabled: Boolean

  def isWarnEnabled: Boolean

  def isTraceEnabled: Boolean
}
