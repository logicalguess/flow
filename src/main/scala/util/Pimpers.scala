package util

import logging.Log

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Try}

object Pimpers {

  implicit class TryPimper[A](t: Try[A]) {
    def withErrorLog(msg: String)(implicit LOG: Log): Try[A] =
      t.recoverWith {
        case e ⇒
          LOG.error(msg, e)
          Failure(e)
      }

    def withFinally[T](block: ⇒ T): Try[A] = {
      block
      t
    }
  }

  implicit class FuturePimper[T](f: Future[T]) {
    def withErrorLog(msg: String)(implicit LOG: Log, ec: ExecutionContext): Future[T] = {
      f.onFailure {
        case e ⇒ LOG.error(msg, e)
      }
      f
    }
  }

  implicit def map2Properties(map: Map[String, String]): java.util.Properties = {
    (new java.util.Properties /: map) { case (props, (k, v)) ⇒ props.put(k, v); props }
  }
}
