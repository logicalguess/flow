package flow.akka

import akka.actor.ActorSystem
import dag.DAG
import util.FunctionImplicits._

object TestMain extends App {

  val graph = DAG("flow",
    List("first"), List("second", "first"), List("third", "second"), List("fourth", "second"),
    List("fifth", "third", "fourth"))


  val constant = {_: Unit => 7}
  val f_str = { i: Int => i.toString }
  val f_bang = {  s: String =>  s + "!" }
  val f_hash = { s: String =>  s + "#" }
  val f_concat = { s: (String, String) => s._1 + s._2 }

  val functions: Map[String, Function[Any, Any]] = Map("first" -> constant,
    "second" -> f_str, "third" -> f_bang,
    "fourth" -> f_hash, "fifth" -> f_concat)

  val system = ActorSystem("DagActorSystem")

  AkkaDAG(graph, functions, Some(system))

  system.awaitTermination()
}