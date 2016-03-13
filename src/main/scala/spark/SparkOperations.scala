package spark

import flow.{TransformerU, Transformer, Operation}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
  * Created by logicalguess on 3/12/16.
  */

//case class SparkOperation[A](f: SparkContext => A) {
//  def apply(sc: SparkContext) = Operation[A] { f(sc) }
//}

case class SparkOperation[A](f: SparkContext => A)
  extends TransformerU[SparkContext, A]

case class RDDTransformer[In, Out](f: RDD[In] => RDD[Out]) {
  def apply(in: RDD[In]) = Operation[RDD[Out]] { f(in) }
}
