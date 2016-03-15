package flow.future

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
    override def apply = f
  }
}

trait TransformerU[In, Out] {
  def f: In => Out
  def apply(in: In) = Operation[Out] { Future { f(in) } }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object Provider {
  def apply[Out](value: Out) = Operation[Out] { Future {value} }
}

