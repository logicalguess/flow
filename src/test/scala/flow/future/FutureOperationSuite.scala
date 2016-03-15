package flow.future

import flow.future.OperationImplicits._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

import scala.util.Success
//import scalaz.syntax.bind._

class FutureOperationSuite extends WordSpec with ShouldMatchers with Logging with ScalaFutures {

  "Implicit examples" should {

    val transformerIntToString : Transformer[Int, String] = { i: Int => i.toString }
    val transformerAppendBang = { s: String => s + "!" }
    val transformerAppendHash = { s: String => s + "#" }
    val transformerConcatenate = { s: (String, String) => s._1 + s._2 }

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
          startOp <- Provider[Int](start)
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
