package flow.future

import flow.future.OperationImplicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

class FutureOperationSuite extends WordSpec with ShouldMatchers with Logging with ScalaFutures {

  val f_str = { i: Int => i.toString }
  val f_bang = { s: String => s + "!" }
  val f_hash = { s: String => s + "#" }
  val f_concat = { s: (String, String) => s._1 + s._2 }

  "function examples" should {

    val logic: String = {
      val i = 7
      val s = f_str(i)
      val b = f_bang(s)
      val h = f_hash(s)
      f_concat(b, h)
    }

    "composition" in {
      logic shouldBe "7!7#"
    }
  }

  "Implicit examples" should {

    val transformerIntToString : Transformer[Int, String] = f_str
    val transformerAppendBang = f_bang
    val transformerAppendHash = f_hash
    val transformerConcatenate = f_concat

    "linear" in  {
      val futureResult = for {
        s <- transformerIntToString(3)
        ss <- transformerAppendBang(s)
      } yield ss

      whenReady(futureResult()) { result =>
        result shouldBe "3!"
      }
    }
  }

  "Transformer examples" should {

    val transformerIntToString = Transformer[Int, String] { i: Int => i.toString }
    val transformerAppendBang = Transformer[String, String] { s: String => s + "!" }
    val transformerAppendHash = Transformer[String, String] { s: String => s + "#" }
    val transformerConcatenate = Transformer[(String, String), String] { s: (String, String) => s._1 + s._2 }

    "linear" in  {

      val futureResult = for {
        s <- transformerIntToString(3)
        ss <- transformerAppendBang(s)
      } yield ss

      whenReady(futureResult()) { result =>
        result shouldBe "3!"
      }

    }

    "diamond" in  {
      def flow(start: Int) = {
        for {
          startOp <- Root[Int](start)
          s1 <- transformerIntToString(startOp)
          s2 <- transformerAppendBang(s1)
          s3 <- transformerAppendHash(s1)
          s4 <- transformerConcatenate(s2, s3)
        } yield s4
      }

      whenReady(flow(7)()) { result =>
        result shouldBe "7!7#"
      }
    }
  }
}
