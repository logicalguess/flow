package spark.sparkjob

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import spark.jobserver._

import scala.util.Try


object WordCount extends SparkJob with NamedRddSupport {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("WordCountExample").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val configString =
      """
        |{
        | inputFile: "inputFile/hosts.txt"
        |}
      """.stripMargin
    val config = ConfigFactory.parseString(configString)
    val results = runJob(sc, config)
    //val results = sparkJobs.run(sc, config)
    println("Result is " + results)
  }
  
  override def validate(sc: SparkContext, config: Config): SparkJobValidation = {
    Try(config.getString("inputFile"))
    .map(x => SparkJobValid)
    .getOrElse(SparkJobInvalid("No inputFile"))
  }
  
  override def runJob(sc: SparkContext, config: Config): Any = {
    val tokenized = sc.textFile(config.getString("inputFile")).flatMap(_.split(" "))
 
    // count the occurrence of each word
    val wordCounts = tokenized.map((_, 1)).reduceByKey(_ + _)
    wordCounts.collect().toMap
  }

  //--------------------

  implicit def namedRDDToRDD[T] (name: String): RDD[T] = this.namedRdds.get[T](name).get

  object FunctionJob {

    def apply[T, Out](rddName: String)(f: RDD[T] => Out): SparkJob with NamedRddSupport = {
      new SparkJob with NamedRddSupport {
        override def runJob(sc: SparkContext, jobConfig: Config): Out = {
          val rdd: RDD[T] = this.namedRdds.get[T](rddName).get
          f(rdd)
        }

        override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid
      }
    }

    def apply[T](rddName: String)(f: (SparkContext, Config) => RDD[T]): SparkJob with NamedRddSupport = {
      new SparkJob with NamedRddSupport {
        override def runJob(sc: SparkContext, jobConfig: Config): String = {
          val rdd: RDD[T] = f(sc, jobConfig)
          this.namedRdds.update(rddName, rdd)
          rddName
        }

        override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid
      }
    }

    def apply[In, Out](inRDDName: String, outRDDName: String)(f: RDD[In] => RDD[Out]): SparkJob with NamedRddSupport = {
      new SparkJob with NamedRddSupport {
        override def runJob(sc: SparkContext, jobConfig: Config): String = {
          val inRDD: RDD[In] = this.namedRdds.get[In](inRDDName).get

          val outRDD: RDD[Out] = f(inRDD)
          this.namedRdds.update(outRDDName, outRDD)
          outRDDName
        }

        override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid
      }
    }
  }


  object sparkJobs {
    import Functions._

    val tokenizerFromConfig: (SparkContext, Config) => RDD[String] = {
      (sc, config) => tokenizer(sc, configValue(config, "inputFile"))
    }

    val tokenizerJob: SparkJob = FunctionJob[String]("tokenized")(tokenizerFromConfig)
    val counterJob: SparkJob = FunctionJob[String, (String, Int)]("tokenized", "wordCounts")(counter)
    val mapperJob: SparkJob = FunctionJob[(String, Int), Map[String, Int]]("wordCounts")(mapper)

    def run(sc: SparkContext, config: Config): Any = {
      tokenizerJob.runJob(sc, config)
      counterJob.runJob(sc, config)
      mapperJob.runJob(sc, config)
    }
  }

  def compose(sc: SparkContext, config: Config): Any = {
    import Functions._

    //mapper(counter(tokenizer(sc, configValue(config, "inputFile"))))

    val tokenized = tokenizer(sc, configValue(config, "inputFile"))
    val wordCounts = counter(tokenized)
    mapper(wordCounts)
  }

  object Functions {
    val configValue: (Config, String) => String = (config, name) => config.getString(name)

    val tokenizer: (SparkContext, String) => RDD[String] =
      (sc, inputFile) => sc.textFile(inputFile).flatMap(_.split(" "))

    val counter: RDD[String] =>  RDD[(String, Int)] =
      tokenized => tokenized.map((_, 1)).reduceByKey(_ + _)

    val mapper: RDD[(String, Int)] => Map[String, Int] =  wordCounts => wordCounts.collect().toMap
  }
}