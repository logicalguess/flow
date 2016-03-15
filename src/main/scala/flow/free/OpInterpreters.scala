package flow.free

import cats._

import scala.concurrent.Future

/**
  * Created by logicalguess on 3/15/16.
  */
object OpInterpreters {

  val idInterpreter = new (External ~> Id) {
    override def apply[A](e: External[A]): Id[A] = e match {
      case Invoke(a) => a
      case InvokeF(f, x) => f(x)
    }
  }

  val futureInterpreter = new (External ~> Future) {
    import scala.concurrent.ExecutionContext.Implicits.global

    override def apply[A](e: External[A]): Future[A] = e match {
      case Invoke(a) => Future {
        a
      }
      case InvokeF(f, x) => Future {
        f(x)
      }
    }
  }


  val logInterpreter = new (External ~> Id) {
    override def apply[A](e: External[A]): Id[A] = e match {
      case Invoke(a) => {
        println("value " + a)
        a
      }
      case InvokeF(f, x) => {
        println("arg " + x)
        f(x)
      }
    }
  }

}
