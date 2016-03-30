package flow

import dag.{Node, DAG}
import util.ParamTuple

sealed trait Operation[A] extends (() => A) {
  def map[B](f: A ⇒ B): Operation[B] = Operation(f(apply()))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(f(apply()).apply()) //f(apply())
  def -->[B] (t: TransformerU[A, B]): Operation[B] = t(apply())
}

object OperationImplicits {
  implicit def Function0ToOperation[A] (f: => A) = Operation(f)
  implicit def Function1ToTransformer[In, Out] (f: In => Out) = Transformer(f)
}

trait LoggingOperation[A] extends Operation[A] {
  abstract override def apply(): A = {
    val res = super.apply()
    println("Logging > " + res)
    res
  }
}

trait LazyOperation[A] extends Operation[A] {
  lazy val value = super.apply()
  abstract override def apply(): A = value
}

class OperationImpl[A](f: => A) extends Operation[A] {
  override def apply = f
}

object Operation {
  def apply[A](f: => A, beLazy: Boolean = true): Operation[A] = beLazy match {
    case true => new OperationImpl[A](f) with LazyOperation[A] with LoggingOperation[A]
    case false => new OperationImpl[A](f) with LoggingOperation[A]
  }

  def sequence[A](list: List[Operation[A]]): Operation[List[A]] = list match {
    case Nil => Operation({Nil})
    case x :: xs => x.flatMap(h => sequence(xs).map(t => h :: t))
  }
}

trait TransformerU[In, Out] {
  def f: In => Out
  def apply(in: In) = Operation[Out] { f(in) }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object OperationBuilder {
  def apply(graph: DAG, values: Map[String, Any],
            functions: Map[String, Function[Any, Any]]): Map[String, Operation[_]] = {

    val ops = collection.mutable.Map[String, Operation[Any]]()

    def build(node: Node): Unit = {
      node.getParents foreach (dep => build(dep))
      val label = node.label

      if (!ops.contains(label)) {

        if (node.isRoot) {
          val value = values(label)
          ops(label) = Operation(value)
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

