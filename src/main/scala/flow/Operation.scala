package flow

//import scalaz._


sealed trait Operation[+A] extends (() => A) {
  //def apply(): A

  // This enables out the box map / flatMap / for comprehension support
  //def map[B](f: A ⇒ B): Operation[B] = Operation.monad.map(this)(f)
  //def flatMap[B](f: A ⇒ Operation[B]): Operation[B] = Operation.monad.bind(this)(f)

  def map[B](f: A ⇒ B): Operation[B] = Operation(f(this.apply()))
  def flatMap[B](f: A => Operation[B]): Operation[B] = Operation(f(apply()).apply())
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

case class Transformer[In, Out](f: In => Out) {
  def apply(in: In) = Operation[Out] { f(in) }
}

object Provider {
  def apply[Out](value: Out) = Operation[Out] { value }
}

