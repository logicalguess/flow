package finatra.data.movielens

import dag.DAG
import finatra.data.DataProvider
import flow.OperationBuilder
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

import util.FunctionImplicits._
import Functions._

/**
  * Created by logicalguess on 1/18/16.
  */
case class MovieLens_100k(sc: SparkContext, dataDirectoryPath: String) extends DataProvider {

  override val graph = Some(DAG("provider",
    List("spark_context"),
    List("ratings_data_source"),
    List("movie_data_source"),
    List("raw_rating_data", "spark_context", "ratings_data_source"),
    List("ratings", "raw_rating_data"),
    List("movies", "spark_context", "movie_data_source"),
    List("movie_names", "movies")))

  val ops = OperationBuilder(graph.get,
    Map("spark_context" -> {
      sc
    },
      "ratings_data_source" -> {
        dataDirectoryPath + "/u.data"
      },
      "movie_data_source" -> {
        dataDirectoryPath + "/u.item"
      }),
    Map("raw_rating_data" -> readFile _,
      "ratings" -> toRatings,
      "movies" -> readFile _,
      "movie_names" -> toNames))

  val t1 = System.currentTimeMillis
  override protected val ratings: RDD[Rating] = ops("ratings")().asInstanceOf[RDD[Rating]]
  override protected val productNames: Map[Int, String] = ops("movie_names")().asInstanceOf[Map[Int, String]]
  override protected val duration = Some(System.currentTimeMillis - t1)

}

object Functions {
  def readFile(sc: SparkContext, file: String): RDD[String] = {
    sc.textFile(file)
  }

  def toRatings(rawData: RDD[String]) = {
    /* Extract the user id, movie id and rating only from the dataset */
    val rawRatings = rawData.map(_.split("\t").take(3))

    /* Construct the RDD of Rating objects */
    val ratings = rawRatings.map {
      case Array(user, movie, rating) => Rating(user.toInt, movie.toInt, rating.toDouble)
    }
    ratings
  }

  val toNames: RDD[String] => Map[Int, String] = { items =>
    val pairRDD: RDD[(Int, String)] = items.map(line => line.split("\\|").take(2))
      .map(array => (array(0).toInt, array(1)))
    pairRDD.collect.toMap

  }
}
