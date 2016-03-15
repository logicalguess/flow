package flow

import cats._
import cats.free.Free
import cats.std.all._
import flow.OpImplicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class OpSuite extends WordSpec with ShouldMatchers with Logging with ScalaFutures {

  val f_str = { i: Int => i.toString }
  val f_bang = { s: String => s + "!" }
  val f_hash = { s: String => s + "#" }
  val f_concat = { s: (String, String) => s._1 + s._2 }

  "function examples" should {

    val logic: String = {
      val i = 5
      val s = f_str(i)
      val b = f_bang(s)
      val h = f_hash(s)
      f_concat(b, h)
    }

    "composition" in {
      logic shouldBe "5!5#"
    }

  }

  "op examples" should {

    val op = Op(5)
    val str: Tr[Int, String] = f_str
    val bang: Tr[String, String] = f_bang
    val hash: Tr[String, String] = f_hash
    val concat: Tr[(String, String), String] = f_concat

    val logic: Free[External, String] = for {
      i <- op
      s <- str(i)
      b <- bang(s)
      h <- hash(s)
      c <- concat(b, h)
    } yield c

    "id" in {

      val idInterpreter = new (External ~> Id) {
        override def apply[A](e: External[A]): Id[A] = e match {
          case Invoke(a) => a
          case InvokeF(f, x) => f(x)
        }
      }

      val result = logic.foldMap(idInterpreter)

      result shouldBe "5!5#"
    }

    "future" in {

      val futureInterpreter = new (External ~> Future) {
        override def apply[A](e: External[A]): Future[A] = e match {
          case Invoke(a) => Future {
            a
          }
          case InvokeF(f, x) => Future {
            f(x)
          }
        }
      }


      val futureResult = logic.foldMap(futureInterpreter)

      whenReady(futureResult) { result =>
        result shouldBe "5!5#"
      }
    }

    "log" in {

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
      logic.foldMap(logInterpreter)
    }
  }
}

