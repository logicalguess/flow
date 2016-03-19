package flow

import dag.{Node, DAG}

sealed trait Operation[A] extends (() => A) {
  def map[B](f: A ⇒ B): Operation[B] = Operation(f(apply()))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(f(apply()).apply()) //f(apply())
  def -->[B] (t: TransformerU[A, B]): Operation[B] = t(apply())
}

object OperationImplicits {
  implicit def Function0ToOperation[A] (f: => A) = Operation(f)
  implicit def Function1ToTransformer[In, Out] (f: In => Out) = Transformer(f)
  implicit def FunctionToPartialFunction[In, Out] (f: Function[In, Out]): PartialFunction[Any, Any] = { case in: In => f(in)}
}

object Operation {
  def apply[A](f: => A): Operation[A] = new Operation[A] {
    override def apply = f
  }
}

trait TransformerU[In, Out] {
  def f: In => Out
  def apply(in: In) = Operation[Out] { f(in) }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object LazyOperation {
  def apply[A](f: => A): Operation[A] = new Operation[A] {
    lazy val value: A = f
    override def apply = value
  }
}

case class TransformerG(f: PartialFunction[Any, Any]) {
  def apply(ops: Seq[Operation[Any]]): Operation[Any] = LazyOperation[Any] {
    ops match {
      case Seq(a) =>  f(a())
      case Seq(a, b) =>  f(a(), b())
      case Seq(a, b, c) =>  f(a(), b(), c())
      case Seq(a, b, c, d) =>  f(a(), b(), c(), d())
      case Seq(a, b, c, d, e) =>  f(a(), b(), c(), d(), e())
      case Seq(a, b, c, d, e, g) =>  f(a(), b(), c(), d(), e(), g())
      case Seq(a, b, c, d, e, g, h) =>  f(a(), b(), c(), d(), e(), g(), h())
    }
  }
}

object OperationBuilder {
  def apply(graph: DAG, values: Map[String, Any],
            functions: Map[String, PartialFunction[Any, Any]]): Map[String, Operation[_]] = {

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
          val op = TransformerG(functions(label))(deps.toList)
          ops(label) = op
        }
      }
    }

    graph.getLeaves foreach( leaf => build(leaf))

    ops.toMap
  }
}

