package util

/**
  * Created by logicalguess on 3/19/16.
  */
object FunctionImplicits {
  implicit def functionToPartialFunction[In, Out] (f: Function[In, Out]): PartialFunction[Any, Any] = { case in: In => f(in)}
}

//val f = m _
//
//// is equivalent to
//
//val f = new AnyRef with Function1[List[Int], AnyRef] {
//  def apply(x$1: List[Int]) = this.m(x$1)
//}
