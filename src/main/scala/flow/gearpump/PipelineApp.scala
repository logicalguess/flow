package flow.gearpump

/**
  * Created by logicalguess on 4/4/16.
  */

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import dag.DAG
import io.gearpump.cluster.UserConfig
import io.gearpump.cluster.client.ClientContext
import io.gearpump.cluster.main.{ArgumentsParser, CLIOption, ParseResult}
import io.gearpump.streaming.StreamApplication
import io.gearpump.util.Graph._
import io.gearpump.util.{AkkaApp, Graph, LogUtil}
import org.slf4j.Logger

import util.FunctionImplicits._


case class PipelineConfig(config: Config) extends java.io.Serializable


object PipelineApp extends AkkaApp with ArgumentsParser {
  private val LOG: Logger = LogUtil.getLogger(getClass)

  override val options: Array[(String, CLIOption[Any])] = Array(
    //"processors"-> CLIOption[Int]("<processor number>", required = false, defaultValue = Some(1)),
   )

  def application(config: ParseResult, system: ActorSystem): StreamApplication = {
    implicit val actorSystem = system
    val pipelineString =
      """
        |pipeline {
        |  processors = 1
        |}
      """.stripMargin

    val pipelineConfig = PipelineConfig(ConfigFactory.parseString(pipelineString))

    val appConfig = UserConfig.empty.withValue[PipelineConfig]("Pipeline", pipelineConfig)

    val graph = DAG("flow", List("first"),
      List("second", "first"), List("third", "second"), List("fourth", "third"))

    val gen = { _: String => (1 to 100).iterator }
    val f_str = { i: Int => i.toString }
    val f_bang = {  s: String =>  s + "!" }
    val f_hash = { s: String =>  s + "#" }

    val functions: Map[String, Function[Any, Any]] = Map(
      "first" -> gen,
      "second" -> f_str,
      "third" -> f_bang,
      "fourth" -> f_hash)

    val app = GearStreamingApp(graph, functions, appConfig)

    println("VERTICES: " + app.dag.vertices.map(v => v.id))
    println("EDGES: " + app.dag.edges.map(e => (e._1.id, e._3.id)))

    app
  }

  override def main(akkaConf: Config, args: Array[String]): Unit = {
    val config = parse(args)
    val context = ClientContext(akkaConf)
    val appId = context.submit(application(config, context.system))
    context.close()
  }

}
