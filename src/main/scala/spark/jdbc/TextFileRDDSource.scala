package spark.jdbc

import org.apache.spark.rdd.RDD
import spark.SparkOperation

object TextFileRDDSource {
  def apply(filename: String, partitions: Int): SparkOperation[RDD[String]] = SparkOperation { ctx ⇒
    ctx.textFile(filename, partitions)
  }
}
