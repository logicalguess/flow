package util

/**
  * Created by logicalguess on 4/24/16.
  */
object Timing {
  def time[T](thunk: => T): (T, Long) = {
    val t1 = System.currentTimeMillis
    val t = thunk
    val t2 = System.currentTimeMillis
    (t, t2 - t1)
  }

}
