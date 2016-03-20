package flow

import java.io.File

import dag.{Connector, DAG, Node, Util}
import util.FunctionImplicits._
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging


class DAGSuite extends WordSpec with ShouldMatchers with Logging {

  "DAG examples" should {
    val constant = {7}
    val f_str = { i: Int => i.toString }
    val f_bang = {  s: String =>  s + "!" }
    val f_hash = { s: String =>  s + "#" }
    val f_concat = { s: (String, String) => s._1 + s._2 }

    "function examples" should {

      val logic: String = {
        val i = constant
        val s = f_str(i)
        val b = f_bang(s)
        val h = f_hash(s)
        f_concat(b, h)
      }

      "composition" in {
        logic shouldBe "7!7#"
      }
    }

    "diamond in code" in {
//      val n1 = Node("first")
//      val n2 = Node("second")
//      val n3 = Node("third")
//      val n4 = Node("fourth")
//      val n5 = Node("fifth")
//
//      val c1 = Connector("first", "second")
//      val c2 = Connector("second", "third")
//      val c3 = Connector("second", "fourth")
//      val c4 = Connector("third", "fifth")
//      val c5 = Connector("fourth", "fifth")
//
//      val graph = DAG("flow", List(n1, n2, n3, n4, n5), List(c1, c2, c3, c4, c5))

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

      ops("fifth")() shouldBe "7!7#"
    }

    "diamond in config" in {

      val graph = DAG.read(new File("src/main/resources/diamond.json"))

      val ops = OperationBuilder(graph,
        Map("first" -> constant),
        Map("second" -> f_str, "third" -> f_bang, "fourth" -> f_hash, "fifth" -> f_concat))

      ops("fifth")() shouldBe "7!7#"
    }

    "diagram" in {

      val graph = DAG.read(new File("src/main/resources/diamond.json"))
      println(graph.getRoots(0).print())

      println(Util.gravizoDotLink(DAG.dotFormatDiagram(graph)))
      //println(Util.teachingmachinesDotLink(DAG.dotFormatDiagram(graph)))
    }
  }
}
