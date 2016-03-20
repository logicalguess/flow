package flow.tryy

import java.io.File

import dag.DAG
import util.FunctionImplicits._
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

import scala.util.Success


class TryDAGSuite extends WordSpec with ShouldMatchers with Logging {

  "DAG examples" should {
    val constant = {7}
    val f_str = { i: Int => i.toString }
    val f_bang = {  s: String => s + "!" }
    val f_hash = { s: String =>  s + "#" }
    val f_concat = { s: (String, String) => s._1 + s._2 }

    "diamond in code" in {

      val graph = DAG("flow",
        List("first"), List("second", "first"), List("third", "second"), List("fourth", "second"),
        List("fifth", "third", "fourth"))

      val ops = OperationBuilder(graph,
        Map("first" -> constant),
        Map("second" -> f_str, "third" -> f_bang, "fourth" -> f_hash, "fifth" -> f_concat))

      println(ops("second")())
      println(ops("third")())
      println(ops("fourth")())
      println(ops("fifth")())

      ops("fifth")() shouldBe Success("7!7#")
    }

    "diamond in config" in {

      val graph = DAG.read(new File("src/main/resources/diamond.json"))

      val ops = OperationBuilder(graph,
        Map("first" -> constant),
        Map("second" -> f_str, "third" -> f_bang, "fourth" -> f_hash, "fifth" -> f_concat))

      ops("fifth")() shouldBe Success("7!7#")
    }
  }
}
