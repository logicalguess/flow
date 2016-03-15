package flow.free

import cats.free.Free

/**
  * Created by logicalguess on 3/14/16.
  */

sealed trait Op[A] extends (() => A) {
  def map[B](f: A ⇒ B): Free[External, B] = Op(f(apply()))
  def flatMap[B](f: A => Op[B]): Free[External, B] = Op(f(apply()).apply())
}

sealed trait External[A]
case class Invoke[A](a: A) extends External[A]
case class InvokeF[In, Out](f: In => Out, in: In) extends External[Out]

object Op {
  def apply[A](f: => A): Free[External, A] = Free.liftF(Invoke(f))
}

trait TrU[In, Out] {
  def f: In => Out
  def apply(in: In): Free[External, Out] = Free.liftF(InvokeF(f, in))
}

case class Tr[In, Out](f: In => Out) extends TrU[In, Out]

object OpImplicits {
  implicit def Function0ToOperation[A](f: => A) = Op(f)
  implicit def Function1ToTransformer[In, Out](f: In => Out) = Tr(f)

}
