package flow

import dag.{DAG, Connector, Node}
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging


class DAGSuite extends WordSpec with ShouldMatchers with Logging {

  "DAG examples" should {

    def pf[X, R](f: Function[X,R]): PartialFunction[Any, Any] = { case x: X => f(x)}

    val intToString = { i: Int => i.toString }
    val appendBang = { s: String => s + "!" }
    val appendHash = { s: String => s + "#" }
    val concat = { s: (String, String) => s._1 + s._2 }

    "diamond" in  {
      val n1 = new Node("first")
      val n2 = new Node("second")
      val n3 = new Node("third")
      val n4 = new Node("fourth")
      val n5 = new Node("fifth")

      val c1 = Connector("first", "second")
      val c2 = Connector("second", "third")
      val c3 = Connector("second", "fourth")
      val c4 = Connector("third", "fifth")
      val c5 = Connector("fourth", "fifth")

      val graph = new DAG("flow", List(n1, n2, n3, n4, n5), List(c1, c2, c3, c4, c5))

      val ops = OperationBuilder(graph,
        Map("second" -> pf(intToString), "third" -> pf(appendBang), "fourth" -> pf(appendHash), "fifth" -> pf(concat)),
        Map("first" -> 7))

      println(ops("second")())
      println(ops("third")())
      println(ops("fourth")())
      println(ops("fifth")())

      ops("fifth")() shouldBe "7!7#"
    }
  }
}
