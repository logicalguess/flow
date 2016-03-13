package flow

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.{ShouldMatchers, WordSpec}
import spark.{RDDTransformer, LOCAL, SparkProvider, SparkOperation}
import util.Logging

import scala.util.{Success, Try}


class SparkSuite extends WordSpec with ShouldMatchers with Logging {

  "Spark examples" should {

    val sc: Operation[SparkContext] = SparkProvider("test")(LOCAL)

    val wordsRDD: SparkOperation[RDD[String]] = SparkOperation[RDD[String]] { sc =>
      sc.makeRDD("There is nothing either good or bad, but thinking makes it so".split(' '))
    }

    val aWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("a")) }
    val countOperation = Transformer[RDD[String], Long] { _.count }

    "words" in new TryExecutor {
      val operation = sc --> wordsRDD // same as wordsRDD(sc)
      val result: Try[RDD[String]] = execute(operation)
      result.get.count() shouldBe 12
    }

    "letter count" in new TryExecutor {
      val flow = for {
        ctx <- sc
        words <- wordsRDD(ctx)
        aWords <- aWords(words)
        count <- countOperation(aWords)
      }
        yield count

      execute(flow) shouldBe Success(2)
    }
  }
}
