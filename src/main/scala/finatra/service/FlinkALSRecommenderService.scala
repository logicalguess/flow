package finatra.service

import javax.inject.{Inject, Singleton}

import dag.{DAG, Util}
import finatra.data.DataProvider
import flow.OperationBuilder
import util.FunctionImplicits._
import org.apache.flink.ml.recommendation.ALS

import scala.util.Random
import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}
import org.apache.flink.api.scala._
import org.apache.spark.mllib.recommendation.Rating
import util.Timing

/**
  * Created by logicalguess on 4/23/16.
  */

@Singleton
case class FlinkALSRecommenderService @Inject()(dataProvider: DataProvider) extends RecommenderService {

  val env = ExecutionEnvironment.getExecutionEnvironment

  val modelExecution: ExecutionInfo = createModel()

    val model: ALS = modelExecution.result.get.asInstanceOf[ALS]


  def getRecommendationsForUser(userId: Int, count: Int) = {

    val userIdFn = {userId}
    val modelFn = {model}
    val productsFn = dataProvider.getProductNames

    val candidatesFn: (Map[Int, String]) => DataSet[Int] = {
      products: Map[Int, String] => env.fromCollection(products.keys)
    }


    val mapByIdFn: (Int, DataSet[Int]) => DataSet[(Int, Int)] =  {
      (userId: Int, ds: DataSet[Int]) => ds.map((userId, _))
    }

    val predictFn: (ALS, DataSet[(Int, Int)]) => Array[(Int, Int, Double)] = {
      (model,  ds: DataSet[(Int, Int)]) =>
        model
        .predict(ds)
        .collect
        .sortBy(- _._3)
        .take(count)
        .toArray
    }
    //val fns = predictFn(mapByIdFn(userId, candidatesFn(sparkContextFn, productsFn)))

    val graph = DAG("flow",
      List("userId"),
      List("movie_names"),
      List("model"),
      List("candidates", "movie_names"),
      List("mapped_by_id", "userId", "candidates"),
      List("predict", "model", "mapped_by_id"))

    val ops = OperationBuilder(graph,
      Map(
        "userId" -> userIdFn,
        "movie_names" -> productsFn,
        "model" -> modelFn),
      Map("candidates" -> candidatesFn,
        "mapped_by_id" -> mapByIdFn,
        "predict" -> predictFn))

    val (recs: List[(Int, Int, Double)], predict_duration: Long) =
      Timing.time {ops("predict")().asInstanceOf[Array[(Int, Int, Double)]].toList }

    List(
      ExecutionInfo("predict", Some(recs.map(x => Rating(x._1, x._2, x._3))), predict_duration, Some(graph)),
      modelExecution.noResult,
      ExecutionInfo("feature", None, dataProvider.getDuration().getOrElse(0), dataProvider.getGraph())
    )
  }

  def createModel(): ExecutionInfo = {
    import FlinkFunctions._

    val ratingsFn: DataSet[Rating] = env.fromCollection(dataProvider.getRatings.collect().toList)

    val graph = DAG("recommender",
      List("ratings"),
      List("dataset", "ratings"),
      List("training", "dataset"),
      List("validation", "dataset"),
      List("testing", "dataset"),
      List("model", "training", "validation"),
      List("rmse", "model", "testing"))


    val ops = OperationBuilder(graph,
      Map("ratings" -> ratingsFn),
      Map(
        "dataset" -> addRandomLongColumnFn,
        "training" -> trainingFilterFn,
        "validation" -> validationFilterFn,
        "testing" -> testingFilterFn,
        "model" -> train,
        "rmse" -> computeRmse _
        )
      )

    val (m: ALS, model_duration: Long) = Timing.time { ops("model")().asInstanceOf[ALS] }

    ExecutionInfo("model", Some(m), model_duration, Some(graph),
      Some("%.3f".format(ops("rmse")().asInstanceOf[Double]).toDouble))
  }
}

object FlinkFunctions {

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: ALS, data: DataSet[(Int, Int, Double)]) = {
    val n = data.count
    val predictions: DataSet[(Int, Int, Double)] = model.predict(data.map(x => (x._1, x._2)))
    val predictionsAndRatings: JoinDataSet[((Int, Int), Double), ((Int, Int), Double)] =
      predictions
        .map(x => ((x._1, x._2), x._3))
        .join(data.map(x => ((x._1, x._2), x._3)))
        .where(0).equalTo(0)

    math.sqrt(predictionsAndRatings.map(x => (x._1._2 - x._2._2) * (x._1._2 - x._2._2)).collect().reduce(_ + _) / n)
  }

  val addRandomLongColumnFn: DataSet[Rating] => DataSet[(Long, Rating)]  = { rs =>
    val rand = new Random()
    rs.map { r => (rand.nextInt(10).toLong, r) }
  }

  val trainingFilterFn: DataSet[(Long, Rating)] => DataSet[(Int, Int, Double)]  = { rs =>
    rs.filter(x => x._1 <= 3).map(x => x._2).map(r => (r.user, r.product, r.rating))
  }
  //val fn: (RDD[(Long, Rating)]) => RDD[Rating] = trainingFilterFn _

  def validationFilterFn(rs: DataSet[(Long, Rating)]): DataSet[(Int, Int, Double)]  = {
    rs.filter(x => x._1 == 4).map(x => x._2).map(r => (r.user, r.product, r.rating))
  }

  def testingFilterFn(rs: DataSet[(Long, Rating)]): DataSet[(Int, Int, Double)]  = {
    rs.filter(x => x._1 == 5).map(x => x._2).map(r => (r.user, r.product, r.rating))
  }

  def train: (DataSet[(Int, Int, Double)], DataSet[(Int, Int, Double)]) =>  ALS = { (validation, training) =>
    val ranks = List(12)
    val lambdas = List(0.1, 10.0)
    val numIters = List(10)

    var bestModel: Option[ALS] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1

    val numValidation = validation.count

    for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
      val estimator = ALS()
        .setNumFactors(rank)
        .setIterations(numIter)
        .setLambda(lambda)

      estimator.fit(training)

      val validationRmse = computeRmse(estimator, validation)

      if (validationRmse < bestValidationRmse) {
        bestModel = Some(estimator)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }
    bestModel.get
  }
}

