package flow

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.{ShouldMatchers, WordSpec}
import spark.{LOCAL, SparkProvider, SparkOperation}
import util.Logging

import scala.util.Try


class SparkSuite extends WordSpec with ShouldMatchers with Logging {

  "Spark examples" should {

    "letters" in new TryExecutor {

      val rdd: SparkOperation[RDD[String]] = SparkOperation[RDD[String]] { sc =>
        sc.makeRDD("There is nothing either good or bad, but thinking makes it so".split(' '))
      }

      val sc: Operation[SparkContext] = SparkProvider("test")(LOCAL)

      val operation = sc --> rdd // same as rdd(sc)
      val result: Try[RDD[String]] = execute(operation)
      result.get.count() shouldBe 12
    }
  }
}
