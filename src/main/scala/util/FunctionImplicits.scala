package util

/**
  * Created by logicalguess on 3/19/16.
  */
object FunctionImplicits {
  implicit def functionToPartialFunction[In, Out] (f: Function[In, Out]): Function[Any, Any] =
    { case in: In => f(in)}
  implicit def function2ToPartialFunction[In1, In2, Out] (f: Function2[In1, In2, Out]): Function[Any, Any] =
    { case in: (In1, In2) => f.tupled(in)}

}

//val f = m _
//
//// is equivalent to
//
//val f = new AnyRef with Function1[List[Int], AnyRef] {
//  def apply(x$1: List[Int]) = this.m(x$1)
//}
