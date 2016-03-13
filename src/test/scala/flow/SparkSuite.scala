package flow

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.{ShouldMatchers, WordSpec}
import spark.{LOCAL, SparkProvider, SparkOperation}
import util.Logging


class SparkSuite extends WordSpec with ShouldMatchers with Logging {
  trait DummyExecutor extends ExecutorU {
    override def execute[A](operation: Operation[A]): A = operation.apply()
  }

  "Spark examples" should {

    "letters" in new DummyExecutor {

      val rdd: SparkOperation[RDD[String]] = SparkOperation[RDD[String]] { sc =>
        sc.makeRDD("There is nothing either good or bad, but thinking makes it so".split(' '))
      }

      val sc: Operation[SparkContext] = SparkProvider("test")(LOCAL)

      execute(rdd(sc()))

      sc --> rdd

    }
  }
}
