package flow.tryy

import flow.tryy.OperationImplicits._
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

import scala.util.Success

class TryOperationSuite extends WordSpec with ShouldMatchers with Logging {

  "Implicit examples" should {

    val transformerIntToString: Transformer[Int, String] = { i: Int => i.toString }
    val transformerAppendBang = { s: String => s + "!" }
    val transformerAppendHash = { s: String => s + "#" }
    val transformerConcatenate = { s: (String, String) => s._1 + s._2 }

    "linear" in {
      val result = for {
        s <- transformerIntToString(3)
        ss <- transformerAppendBang(s)
      } yield ss

      result() shouldBe Success("3!")
    }
  }

  "Transformer examples" should {

    val transformerIntToString = Transformer[Int, String] { i: Int => i.toString }
    val transformerAppendBang = Transformer[String, String] { s: String => s + "!" }
    val transformerAppendHash = Transformer[String, String] { s: String => s + "#" }
    val transformerConcatenate = Transformer[(String, String), String] { s: (String, String) => s._1 + s._2 }

    "linear" in {

      val result = for {
        s <- transformerIntToString(3)
        ss <- transformerAppendBang(s)
      } yield ss

      result() shouldBe Success("3!")
    }

    "diamond" in {
      def flow(start: Int) = {
        for {
          startOp <- Root[Int](start)
          s1 <- transformerIntToString(startOp)
          s2 <- transformerAppendBang(s1)
          s3 <- transformerAppendHash(s1)
          s4 <- transformerConcatenate(s2, s3)
        } yield s4
      }

      flow(7)() shouldBe Success("7!7#")
    }
  }
}
