package flow

import logging.SparkLogging
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.{ShouldMatchers, WordSpec}
import spark.{LOCAL, RDDTransformer, SparkOperation, SparkProvider}
import org.scalatest.concurrent.ScalaFutures
import org.apache.log4j.Logger
import org.apache.log4j.Level


class SparkSuite extends WordSpec with ShouldMatchers with SparkLogging with ScalaFutures {

  Logger.getRootLogger.setLevel(Level.WARN)
  Logger.getLogger("org").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("spark").setLevel(Level.WARN)

  "Spark examples" should {

    val scStart: Operation[SparkContext] = SparkProvider("test")(LOCAL)

    val scStop = Transformer[SparkContext, Unit] { _.stop() }


    val wordsRDD: SparkOperation[RDD[String]] = SparkOperation[RDD[String]] { sc =>
      sc.makeRDD("There is nothing either good or bad, but thinking makes it so".split(' '))
    }

    val aWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("a")) }
    val bWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("b")) }

    val countOperation = Transformer[RDD[String], Long] { _.count }

    "words" in  {
      val operation = scStart --> wordsRDD // same as wordsRDD(sc)
      val result: RDD[String] = operation()
      result.count() shouldBe 12
    }

    "letter count" in  {

      val flow = for {
        sc <- scStart
        words <- wordsRDD(sc)

        aWords <- aWords(words)
        countA <- countOperation(aWords)

        bWords <- bWords(words)
        countB <- countOperation(bWords)

        _ <- scStop(sc)
      }
        yield (countA, countB)

        //scStart().stop()
        flow() shouldBe (2, 2)
    }
  }
}
