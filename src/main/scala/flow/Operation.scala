package flow

import dag.{Connector, Node, DAG}

import scala.collection.mutable.ArrayBuffer

//import scalaz._

sealed trait Operation[A] extends (() => A) {
  //def apply(): A

  // This enables out the box map / flatMap / for comprehension support
  //def map[B](f: A ⇒ B): Operation[B] = Operation.monad.map(this)(f)
  //def flatMap[B](f: A ⇒ Operation[B]): Operation[B] = Operation.monad.bind(this)(f)

  def map[B](f: A ⇒ B): Operation[B] = Operation(f(apply()))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(f(apply()).apply())

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

//  implicit val monad = new Monad[Operation] {
//    override def bind[A, B](fa: Operation[A])(f: A ⇒ Operation[B]): Operation[B] =
//      Operation(f(fa.apply()).apply())
//
//    override def point[A](a: ⇒ A): Operation[A] = Operation(a)
//  }
}

trait TransformerU[In, Out] {
  def f: In => Out
  def apply(in: In) = Operation[Out] { f(in) }
//  def apply(l: Seq[Any]) = Operation[Out] {
//    l match {
//      case Seq(a) =>  f((a).asInstanceOf[In])
//      case Seq(a, b) =>  f((a, b).asInstanceOf[In])
//    }
//  }
}

case class TransformerG(f: PartialFunction[Any, Any]) {
  def apply(ops: Seq[Operation[Any]]): Operation[Any] = Operation[Any] {
    ops match {
      case Seq(a) =>  f((a()))
      case Seq(a, b) =>  f((a(), b()))
    }
  }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object Root {
  def apply[Out](value: Out) = Operation[Out] { value }
}

object OperationBuilder {
  def apply(graph: DAG, functions: Map[String, PartialFunction[Any, Any]],
            values: Map[String, Any]): Map[String, Operation[_]] = {

    val ops = collection.mutable.Map[String, Operation[Any]]()

    def build(node: Node): Unit = {
      node.getParents foreach (dep => build(dep))
      val label = node.label

      if (!ops.contains(label)) {

        if (node.isRoot) {
          val value = values(label)
          println(value.getClass)
          ops(label) = /*if (value.isInstanceOf[Function[_, _]])*/ Operation(value) //else Root(value)
        }
        else {
          val deps = node.getParentLabels collect ops
          val op = TransformerG(functions(label)).apply(deps.toList)
          ops(label) = op
        }
      }
    }

    graph.getLeaves foreach( leaf => build(leaf))

    ops.toMap
  }
}

