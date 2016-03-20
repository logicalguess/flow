package util

/**
  * Created by logicalguess on 3/19/16.
  */

object ParamTuple {
  def apply[A, B](f: (Any => _)): (List[Any] => _) = { args: List[Any] =>
    args match {
      case List(a) =>  f(a)
      case List(a, b) =>  f(a, b)
      case List(a, b, c) =>  f(a, b, c)
      case List(a, b, c, d) =>  f(a, b, c, d)
      case List(a, b, c, d, e) =>  f(a, b, c, d, e)
      case List(a, b, c, d, e, g) =>  f(a, b, c, d, e, g)
      case List(a, b, c, d, e, g, h) =>  f(a, b, c, d, e, g, h)
    }
  }
}


object Main {
  val f : ((Int, String, Int)) =>  Unit = { arg: ((Int, String, Int)) =>
    println(arg)
    arg._1 + arg._3
  }

  def main (args: Array[String] ) {
    val f_lift = ParamTuple(f.asInstanceOf[(Any => _)])

    f_lift(List(1, "b", 3))
  }
}
