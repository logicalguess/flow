package finatra.service

import javax.inject.Singleton

import dag.{Util, DAG, Connector, Node}
import finatra.views.FlowResult
import flow.OperationBuilder
import util.FunctionImplicits._


/**
  * Created by logicalguess on 3/18/16.
  */

@Singleton
class FlowService {

  val n1 = Node("int")
  val n2 = Node("int_to_string")
  val n3 = Node("append_bang")
  val n4 = Node("append_hash")
  val n5 = Node("concat")

  val c1 = Connector("int", "int_to_string")
  val c2 = Connector("int_to_string", "append_bang")
  val c3 = Connector("int_to_string", "append_hash")
  val c4 = Connector("append_bang", "concat")
  val c5 = Connector("append_hash", "concat")

  val graph = DAG("diamond", List(n1, n2, n3, n4, n5), List(c1, c2, c3, c4, c5))


  val f_str = { i: Int => i.toString }
  val f_bang = { s: String => s + "!" }
  val f_hash = { s: String => s + "#" }
  val f_concat = { s: (String, String) => s._1 + s._2 }

  def runDiamond(start: Int): FlowResult = {

    val constant = {start}

    import flow.OperationImplicits._

    val ops = OperationBuilder(graph,
      Map("int" -> constant),
      Map("int_to_string" -> f_str, "append_bang" -> f_bang, "append_hash" -> f_hash, "concat" -> f_concat))

    FlowResult(ops("concat")(), Util.gravizoDotLink(DAG.dotFormatDiagram(graph)))


  }

}
