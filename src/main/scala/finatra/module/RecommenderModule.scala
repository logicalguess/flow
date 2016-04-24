package finatra.module

import javax.inject.Singleton

import com.google.inject.{Inject, Provides}
import com.twitter.inject.TwitterModule
import finatra.data.DataProvider
import finatra.data.movielens.{MovieLens_100k, MovieLens_1m}
import finatra.service.{ALSRecommenderService, FlinkALSRecommenderService, FlowALSRecommenderService, RecommenderService}
import org.apache.spark.SparkContext

object RecommenderModule extends TwitterModule {
  flag("rec.count", 10, "number of recommendations to be returned")

  private val dataSet = flag("data.set", "100k", "1m or 100k")
  private val recType = flag("rec.type", "flink", "basic or flow or flink")


  val MOVIE_LENS_1M = "1m"
  val MOVIE_LENS_100K = "100k"

  val REC_TYPE_BASIC = "basic"
  val REC_TYPE_FLOW = "flow"
  val REC_TYPE_FLINK = "flink"


  @Singleton
  @Provides
  def dataProvider(@Inject() sco: Option[SparkContext]): DataProvider = {
    println("------------------data provider init-------------------")
    dataSet() match {
      case MOVIE_LENS_1M => MovieLens_1m(sco.get, "src/main/resources/ml-1m")
      case MOVIE_LENS_100K => MovieLens_100k(sco.get, "src/main/resources/ml-100k")
      case _ => throw new IllegalArgumentException("unknown data set")
    }
  }

  @Singleton
  @Provides
  def recommenderProvider(@Inject() sco: Option[SparkContext], dataProvider: DataProvider): RecommenderService = {

    recType() match {
      case REC_TYPE_BASIC => ALSRecommenderService(sco.get, dataProvider)
      case REC_TYPE_FLOW => FlowALSRecommenderService(sco.get, dataProvider)
      case REC_TYPE_FLINK => FlinkALSRecommenderService(dataProvider)
      case _ => throw new IllegalArgumentException("unknown data set")
    }
  }
}
