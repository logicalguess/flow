package flow

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
//  def apply(l: Seq[Any]) = {
//    l match {
//      case Seq(a) =>  f((a).asInstanceOf[In])
//      case Seq(a, b) =>  f((a, b).asInstanceOf[In])
//    }
//  }
}

case class Transformer[In, Out](f: In => Out) extends TransformerU[In, Out]

object Root {
  def apply[Out](value: Out) = Operation[Out] { value }
}

