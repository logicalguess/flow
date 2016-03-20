package util

/**
  * Created by logicalguess on 3/19/16.
  */
object FunctionImplicits {
  implicit def FunctionToPartialFunction[In, Out] (f: Function[In, Out]): PartialFunction[Any, Any] = { case in: In => f(in)}
}
