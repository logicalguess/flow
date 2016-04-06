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
      taskContext.output(Message(fun(msg.msg), System.currentTimeMillis))
    }) match {
      case Success(ok) =>
      case Failure(throwable) =>
        LOG.error(s"failed ${throwable.getMessage}")
    }
  }
}