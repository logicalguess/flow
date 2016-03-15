package flow.tryy

import scala.util.Try


sealed trait Operation[A] extends (() => Try[A]) {

  def map[B](f: A ⇒ B): Operation[B] = Operation(apply().map(f))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(apply().flatMap(a => f(a)()))

  def -->[B] (t: TransformerU[A, B]): Operation[B] = Operation(apply().flatMap(a => t(a)()))
}

object OperationImplicits {
  implicit def Function0ToOperation[A] (f: => A) = Operation(Try{f})
  implicit def Function1ToTransformer[In, Out] (f: In => Out) = Transformer[In, Out](in => f(in))
}

object Operation {
  def apply[A](f: => Try[A]): Operation[A] = new Operation[A] {
    override def apply = f
  }
}

trait TransformerU[In, Out] {
  def f: In => Out
  def apply(in: In) = Operation[Out] { Try { f(in) } }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object Provider {
  def apply[Out](value: Out) = Operation[Out] { Try {value} }
}

