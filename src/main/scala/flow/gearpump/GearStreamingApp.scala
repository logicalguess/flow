package flow.gearpump

import akka.actor.ActorSystem
import dag.{DAG, Node}
import io.gearpump.cluster.UserConfig
import io.gearpump.partitioner.HashPartitioner
import io.gearpump.streaming.{Processor, StreamApplication}
import io.gearpump.util.Graph
import io.gearpump.util.Graph._
import util.FunctionImplicits._


/**
  * Created by logicalguess on 4/4/16.
  */

object GearStreamingApp {
  private def configFunction(fun: Function[Any, Any], config: UserConfig = UserConfig.empty)(implicit sys : ActorSystem): UserConfig = {
    val system: ActorSystem = implicitly[ActorSystem]
    config.withValue[Function[Any, Any]]("function", fun)
  }

  def apply(graph: DAG, functions: Map[String, Function[Any, Any]], config: UserConfig = UserConfig.empty)(implicit sys : ActorSystem): StreamApplication = {
    val system: ActorSystem = implicitly[ActorSystem]

    val g: Graph[Processor[GearTask], HashPartitioner] = Graph()

    val partitioner = new HashPartitioner

    val vertices = scala.collection.mutable.Map[String, Processor[GearTask]]()

    for (node <- graph.nodes) {
      val label: String  = node.label
      val processor: Processor[GearTask] = Processor[GearTask](1, label, configFunction(functions(label), config)
        .withBoolean("root", node.isRoot))
      vertices(label) = processor
      g.addVertex(processor)
    }

    for(edge <- graph.connectors) {
      val node1: Node = graph.getNode(edge.from)
      val label1 = node1.label
      val p1 = vertices(label1)
      val label2 = graph.getNode(edge.to).label
      val p2 = vertices(label2)
      g.addEdge(p1, partitioner, p2)
    }

    StreamApplication("Test", g, config)
  }
}