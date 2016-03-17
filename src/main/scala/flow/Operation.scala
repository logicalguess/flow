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
    //val vals = collection.mutable.Map[String, Any]() ++= values


    def build(node: Node): Unit = {
      node.getParents foreach (dep => build(dep))

      val label = node.label

      if (!ops.contains(label)) {

        if (node.isRoot) {
          ops(label) = Root(values(label))
        }
        else {
          //val depValues = node.getParentLabels collect vals
          val deps = node.getParentLabels collect ops
          val op = TransformerG(functions(label)).apply(deps.toList)
          ops(label) = op
          //vals(label) = op()
        }
      }
    }

    graph.getLeaves foreach( leaf => build(leaf))

    ops.toMap
  }
}

object Main extends App {
  def pf[X, R](f: Function[X,R]): PartialFunction[Any, Any] =
  { case x: X => f(x)}

  val n1 = new Node("first")
  val n2 = new Node("second")
  val n3 = new Node("third")
  val n4 = new Node("fourth")
  val n5 = new Node("fifth")

  val c1 = Connector("first", "second")
  val c2 = Connector("second", "third")
  val c3 = Connector("second", "fourth")
  val c4 = Connector("third", "fifth")
  val c5 = Connector("fourth", "fifth")

  val graph = new DAG("flow", false, 1, List(n1, n2, n3, n4, n5), List(c1, c2, c3, c4, c5))

  val intToString = { i: Int => i.toString }

  val appendBang = { s: String => s + "!" }

  val appendHash = { s: String => s + "#" }

  val concat = { s: (String, String) => s._1 + s._2 }

  val ops = OperationBuilder(graph,
    Map("second" -> pf(intToString), "third" -> pf(appendBang), "fourth" -> pf(appendHash), "fifth" -> pf(concat)),
    Map("first" -> 7))
  
  println(ops("second")())
  println(ops("third")())
  println(ops("fourth")())
  println(ops("fifth")())
}

