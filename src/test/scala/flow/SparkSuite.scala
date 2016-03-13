package flow

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.{ShouldMatchers, WordSpec}
import spark.{RDDTransformer, LOCAL, SparkProvider, SparkOperation}
import util.Logging
import org.scalatest.concurrent.ScalaFutures

import scala.util.Try


class SparkSuite extends WordSpec with ShouldMatchers with Logging with ScalaFutures {

  "Spark examples" should {

    val scStart: Operation[SparkContext] = SparkProvider("test")(LOCAL)

    val scStop = Transformer[SparkContext, Unit] { _.stop() }


    val wordsRDD: SparkOperation[RDD[String]] = SparkOperation[RDD[String]] { sc =>
      sc.makeRDD("There is nothing either good or bad, but thinking makes it so".split(' '))
    }

    val aWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("a")) }
    val bWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("b")) }

    val countOperation = Transformer[RDD[String], Long] { _.count }

    "words" in new TryExecutor {
      val operation = scStart --> wordsRDD // same as wordsRDD(sc)
      val result: Try[RDD[String]] = execute(operation)
      result.get.count() shouldBe 12
    }

    "letter count" in new FutureExecutor {
      import scala.concurrent.ExecutionContext.Implicits.global

      val flow = for {
        sc <- scStart
        words <- wordsRDD(sc)

        aWords <- aWords(words)
        countA <- countOperation(aWords)

        bWords <- bWords(words)
        countB <- countOperation(bWords)

        //_ <- scStop(sc)
      }
        yield (countA, countB)

      val futureResult = execute(flow)

      whenReady(futureResult) { result =>
        scStart().stop()
        result shouldBe (2, 2)
      }
    }
  }
}
