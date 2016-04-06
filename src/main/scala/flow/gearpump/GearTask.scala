package flow.gearpump

/**
  * Created by logicalguess on 4/4/16.
  */

import io.gearpump.Message
import io.gearpump.cluster.UserConfig
import io.gearpump.streaming.task.{StartTime, Task, TaskContext}

import scala.util.{Failure, Success, Try}

class GearTask(taskContext : TaskContext, config: UserConfig) extends Task(taskContext, config) {
  val START = null
  val fun: Function[Any, Any] = config.getValue[Function[Any, Any]]("function").get

  override def onStart(startTime: StartTime): Unit = {
    if (config.getBoolean("root").get) self ! Message(START, System.currentTimeMillis)
  }

  override def onNext(msg: Message): Unit = {
    Try( {
      LOG.debug(s"got message ${msg}")
      val res: Any = fun(msg.msg)
      if (res.isInstanceOf[Iterator[_]]) {
        val it: Iterator[_] = res.asInstanceOf[Iterator[_]]
        it.foreach( m => taskContext.output(Message(m, System.currentTimeMillis)))
      }
      else
        taskContext.output(Message(res, System.currentTimeMillis))
    }) match {
      case Success(ok) =>
      case Failure(throwable) =>
        LOG.error(s"failed ${throwable.getMessage}")
    }
  }
}