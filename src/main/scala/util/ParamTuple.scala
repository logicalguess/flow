package util

/**
  * Created by logicalguess on 3/19/16.
  */

object ParamTuple {
  def apply[In, Out](ff: (In => Out)): (List[_] => Out) = { args: List[_] =>
    val f = ff.asInstanceOf[Any => Out]
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
  val f : ((Int, String, Int)) =>  Int = { arg: ((Int, String, Int)) =>
    println(arg)
    arg._1 + arg._3
  }

  def main (args: Array[String] ) {
    val f_lift: List[_] => Int = ParamTuple(f)

    println(f_lift(List(1, "b", 3)))
  }
}

//val xs: Seq[Any] = List(1:Int, 2.0:Double, "3":String)
//val t: (Int,Double,String) = xs.foldLeft((Tuple3[Int,Double,String] _).curried:Any)({
//case (f,x) => f.asInstanceOf[Any=>Any](x)
//}).asInstanceOf[(Int,Double,String)]
