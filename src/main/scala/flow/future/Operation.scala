package flow.future

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


//import scalaz._


sealed trait Operation[A] extends (() => Future[A]) {
  //def apply(): A

  // This enables out the box map / flatMap / for comprehension support
  //def map[B](f: A ⇒ B): Operation[B] = Operation.monad.map(this)(f)
  //def flatMap[B](f: A ⇒ Operation[B]): Operation[B] = Operation.monad.bind(this)(f)

  def map[B](f: A ⇒ B): Operation[B] = Operation(apply().map(f))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(apply().flatMap( a => f(a)())) //Operation(f(apply()).apply())

  def -->[B] (t: TransformerU[A, B]): Operation[B] = {
//    val fa: Future[A] = apply()
//    val fun : A=>Future[B] =  {a: A => t(a)()}
//    val r: Future[B] = fa.flatMap(fun)
//    Operation(r)
    Operation(apply().flatMap({a: A => t(a)()}))
  } //t(apply())
}

object OperationImplicits {
  implicit def Function0ToOperation[A] (f: => A) = Operation(Future{f})
  implicit def Function1ToTransformer[In, Out] (f: In => Out) = {Transformer[In, Out](in => f(in))
  }

}

object Operation {
//  def apply[A](f: => A): Operation[A] = new Operation[A] {
//    override def apply = Future { f }
//  }

  def apply[A](f: => Future[A]): Operation[A] = new Operation[A] {
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
  def apply(in: In) = Operation[Out] { Future { f(in) } }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object Provider {
  def apply[Out](value: Out) = Operation[Out] { Future{value} }
}

