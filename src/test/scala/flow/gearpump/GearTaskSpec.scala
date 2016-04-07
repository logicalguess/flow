package flow.gearpump

/**
  * Created by logicalguess on 4/4/16.
  */


import akka.actor.ActorSystem
import dag.DAG
import io.gearpump.Message
import io.gearpump.cluster.{TestUtil, UserConfig}
import io.gearpump.cluster.client.ClientContext
import io.gearpump.streaming.task.{StartTime, TaskContext}
import io.gearpump.streaming.{MockUtil, Processor, StreamApplication}
import io.gearpump.util.Graph
import io.gearpump.util.Graph._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{BeforeAndAfterAll, Matchers, PropSpec}
import util.FunctionImplicits._

class GearTaskSpec extends PropSpec with PropertyChecks with Matchers with BeforeAndAfterAll with MockitoSugar  {

  //implicit var system: ActorSystem = ActorSystem("GearTaskSpec")
  implicit var system: ActorSystem = null

  val appName = "GearTest"
  val now = System.currentTimeMillis

  val userConfig = UserConfig.empty

  def configFunction(fun: Function[Any, Any], userConfig: UserConfig = UserConfig.empty): UserConfig = {
    userConfig.withValue[Function[Any, Any]]("function", fun)
  }

  override def beforeAll: Unit = {
    system = ActorSystem("test",  TestUtil.DEFAULT_CONFIG)
  }

  override def afterAll(): Unit = {
    //system.awaitTermination()
    system.shutdown()
  }

  property("Task") {
    val context = MockUtil.mockTaskContext

    val message = Message(7, now)
    val fun: Int => String = { i => "done" }
    val task = new GearTask(context, configFunction(fun))
    task.onNext(message)
    verify(context, times(1)).output(Message("done", anyLong()))
    task.onStop
  }

  property("Generator root") {
    val context = MockUtil.mockTaskContext
    val fun: String => Iterator[_] = { _ => (1 to 100).iterator }
    val task = new GearTask(context, configFunction(fun).withBoolean("root", true))
    task.onStart(StartTime(now))
    verify(context, times(100)).output(anyObject())
    task.onStop
  }

  property("Generator non-root") {
    val context = MockUtil.mockTaskContext
    val fun: String => Iterator[_] = { _ => (1 to 100).iterator }
    val task = new GearTask(context, configFunction(fun).withBoolean("root", false))
    task.onStart(StartTime(now))
    verify(context, times(0)).output(anyObject())
    task.onStop
  }

  property("App") {
    val appConfig = UserConfig.empty

    val gearTask1 = Processor[GearTask](1, "GearTask", configFunction({ a => 0} ))
    val gearTask2 = Processor[GearTask](1, "GearTask", configFunction({ a =>  a.asInstanceOf[Int] + 1 }))

    val graph = Graph(
      gearTask1 ~> gearTask2
    )

    val app = StreamApplication("Test", graph, appConfig)

    val ctx = mock[ClientContext]
    when(ctx.system).thenReturn(system)
    val appId = ctx.submit(app)

    ctx.close()
  }

  property("DAG") {
    val graph = DAG("flow",
      List("second"), List("third", "second"), List("fourth", "third"))

    val f_str = { i: Int => i.toString }
    val f_bang = {  s: String =>  s + "!" }
    val f_hash = { s: String =>  s + "#" }

    val functions: Map[String, Function[Any, Any]] = Map(
      "second" -> f_str, "third" -> f_bang,
      "fourth" -> f_hash)

    implicit val system = ActorSystem("test",  TestUtil.DEFAULT_CONFIG)

    val app = GearStreamingApp(graph, functions)

    val ctx = mock[ClientContext]
    when(ctx.system).thenReturn(system)
    val appId = ctx.submit(app)

    ctx.close()
  }

}
