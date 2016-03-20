package flow.future

import dag.{Node, DAG}
import util.ParamTuple

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait Operation[A] extends (() => Future[A]) {

  def map[B](f: A ⇒ B): Operation[B] = Operation(apply().map(f))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(apply().flatMap(a => f(a)()))

  def -->[B] (t: TransformerU[A, B]): Operation[B] = Operation(apply().flatMap(a => t(a)()))
}

object OperationImplicits {
  implicit def Function0ToOperation[A] (f: => A) = Operation(Future{f})
  implicit def Function1ToTransformer[In, Out] (f: In => Out) = Transformer[In, Out](in => f(in))
}

object Operation {
  def apply[A](f: => Future[A]): Operation[A] = new Operation[A] {
    lazy val value: Future[A] = f
    override def apply = value
  }
  def sequence[A](list: List[Operation[A]]): Operation[List[A]] = list match {
    case Nil => Operation(Future{Nil})
    case x :: xs => x.flatMap(h => sequence(xs).map(t => h :: t))
  }
}

trait TransformerU[In, Out] {
  def f: In => Out
  def apply(in: In) = Operation[Out] { Future { f(in) } }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object Root {
  def apply[Out](value: Out) = Operation[Out] { Future {value} }
}

object OperationBuilder {
  def apply(graph: DAG, values: Map[String, Any],
            functions: Map[String, ((Any) => _)]): Map[String, Operation[_]] = {

    val ops = collection.mutable.Map[String, Operation[Any]]()

    def build(node: Node): Unit = {
      node.getParents foreach (dep => build(dep))
      val label = node.label

      if (!ops.contains(label)) {

        if (node.isRoot) {
          val value = values(label)
          ops(label) = Operation(Future{value})
        }
        else {
          val deps = node.getParentLabels collect ops
          ops(label) = Operation.sequence(deps.toList).map(ParamTuple(functions(label)))
        }
      }
    }

    graph.getLeaves foreach( leaf => build(leaf))

    ops.toMap
  }
}
