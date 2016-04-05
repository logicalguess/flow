package flow.gearpump

import akka.actor.ActorSystem
import dag.DAG
import io.gearpump.cluster.UserConfig
import io.gearpump.partitioner.HashPartitioner
import io.gearpump.streaming.Processor.DefaultProcessor
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

    val edges = for {
      edge <- graph.connectors
      label1 = graph.getNode(edge.from).label
      p1 = Processor[GearTask](1, label1, configFunction(functions(label1), config))
      label2 = graph.getNode(edge.to).label
      p2 = Processor[GearTask](1, label2, configFunction(functions(label2), config))
    } yield  (p1, p2)

    edges.foreach( e => g.addEdge(e._1, partitioner, e._2))

    StreamApplication("Test", g, config)
  }
}