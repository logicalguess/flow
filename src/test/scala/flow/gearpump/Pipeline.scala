package flow.gearpump

/**
  * Created by logicalguess on 4/4/16.
  */

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import io.gearpump.cluster.UserConfig
import io.gearpump.cluster.client.ClientContext
import io.gearpump.cluster.main.{ArgumentsParser, CLIOption, ParseResult}
import io.gearpump.streaming.{Processor, StreamApplication}
import io.gearpump.util.Graph._
import io.gearpump.util.{AkkaApp, Graph, LogUtil}
import org.slf4j.Logger


case class PipelineConfig(config: Config) extends java.io.Serializable

object Pipeline extends AkkaApp with ArgumentsParser {
  private val LOG: Logger = LogUtil.getLogger(getClass)

  override val options: Array[(String, CLIOption[Any])] = Array(
    //"processors"-> CLIOption[Int]("<processor number>", required = false, defaultValue = Some(1)),
   )

  def application(config: ParseResult, system: ActorSystem): StreamApplication = {
    implicit val actorSystem = system
    val pipelineString =
      """
        |pipeline {
        |  cpu.interval = 20
        |  memory.interval = 20
        |  processors = 1
        |  persistors = 1
        |}
        |hbase {
        |  table {
        |    name = "pipeline"
        |    column {
        |      family = "metrics"
        |      name = "average"
        |    }
        |  }
        |}
      """.stripMargin
    val pipelineConfig = PipelineConfig(ConfigFactory.parseString(pipelineString))

    val appConfig = UserConfig.empty.withValue[PipelineConfig]("Pipeline", pipelineConfig)

    val gearTask1 = Processor[GearTask](1, "GearTask")
    val gearTask2 = Processor[GearTask](1, "GearTask")

    val graph = Graph(
      gearTask1 ~> gearTask2
    )
    val app = StreamApplication("Test", graph, appConfig)
    app
  }

  override def main(akkaConf: Config, args: Array[String]): Unit = {
    val config = parse(args)
    val context = ClientContext(akkaConf)
    val appId = context.submit(application(config, context.system))
    context.close()
  }

}
