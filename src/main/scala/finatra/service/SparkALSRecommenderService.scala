package finatra.service

import javax.inject.{Inject, Singleton}

import dag.DAG
import finatra.data.DataProvider
import flow.OperationBuilder
import util.FunctionImplicits._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

import scala.util.Random
import Functions._
import util.Timing

/**
  * Created by logicalguess on 2/26/16.
  */

@Singleton
case class SparkALSRecommenderService @Inject()(sc: SparkContext, dataProvider: DataProvider) extends RecommenderService {

  val modelExecution: ExecutionInfo = createModel()

  val model: MatrixFactorizationModel = modelExecution.result.get.asInstanceOf[MatrixFactorizationModel]

  def getRecommendationsForUser(userId: Int, count: Int) = {

    val userId = {userId}
    val sparkContext = {sc}
    val model = {model}
    val products = dataProvider.getProductNames

    val candidates: (SparkContext, Map[Int, String]) => RDD[Int] = {
      (sc: SparkContext, products: Map[Int, String]) => sc.parallelize(products.keys.toSeq).cache()
    }

    val mapById: (Int, RDD[Int]) => RDD[(Int, Int)] =  {
      (userId: Int, rdd: RDD[Int]) => rdd.map((userId, _)).cache()
    }

    val predict: (MatrixFactorizationModel, RDD[(Int, Int)]) => Array[Rating] = {
      (model,  rdd: RDD[(Int, Int)]) =>
        model
        .predict(rdd)
        .collect
        .sortBy(- _.rating)
        .take(count)
    }
    //val fns = predict(mapById(userId, candidates(sparkContext, products)))

    val graph = DAG("flow",
      List("userId"),
      List("spark_context"),
      List("movie_names"),
      List("model"),
      List("candidates", "spark_context", "movie_names"),
      List("mapped_by_id", "userId", "candidates"),
      List("predict", "model", "mapped_by_id"))

    val ops = OperationBuilder(graph,
      Map("spark_context" -> sparkContext,
        "userId" -> userId,
        "movie_names" -> products,
        "model" -> model),
      Map("candidates" -> candidates,
        "mapped_by_id" -> mapById,
        "predict" -> predict))

    val (recs: List[Rating], predict_duration: Long) = Timing.time {ops("predict")().asInstanceOf[Array[Rating]].toList }

    List(
      ExecutionInfo("predict", Some(recs), predict_duration, Some(graph)),
      modelExecution.noResult,
      ExecutionInfo("feature", None, dataProvider.getDuration().getOrElse(0), dataProvider.getGraph())
    )
  }

  def createModel(): ExecutionInfo = {

    val ratings: RDD[Rating] = dataProvider.getRatings

//    val ratings = addRandomLongColumn(ratings)
//    val model = train(trainingFilter(ratings), validationFilter(ratings))
//    val testRmse = computeRmse(model, testingFilter(ratings))
//    (model, testRmse)

    val graph = DAG("recommender",
      List("ratings"),
      List("dataset", "ratings"),
      List("training", "dataset"),
      List("validation", "dataset"),
      List("testing", "dataset"),
      List("model", "training", "validation"),
      List("rmse", "model", "testing"))


    val ops = OperationBuilder(graph,
      Map("ratings" -> ratings),
      Map(
        "dataset" -> addRandomLongColumn,
        "training" -> trainingFilter,
        "validation" -> validationFilter,
        "testing" -> testingFilter,
        "model" -> train,
        "rmse" -> computeRmse _
        )
      )

    val (m: MatrixFactorizationModel, duration: Long) = Timing.time {ops("model")().asInstanceOf[MatrixFactorizationModel]}

    ExecutionInfo("model", Some(m), duration, Some(graph),
      Some("%.3f".format(ops("rmse")().asInstanceOf[Double]).toDouble))
  }
}

object Functions {

  val numPartitions = 20

  /** Compute RMSE (Root Mean Squared Error). */
  def computeRmse(model: MatrixFactorizationModel, data: RDD[Rating]) = {
    val n = data.count
    val predictions: RDD[Rating] = model.predict(data.map(x => (x.user, x.product)))
    val predictionsAndRatings = predictions.map(x => ((x.user, x.product), x.rating))
      .join(data.map(x => ((x.user, x.product), x.rating)))
      .values
    math.sqrt(predictionsAndRatings.map(x => (x._1 - x._2) * (x._1 - x._2)).reduce(_ + _) / n)
  }


//  def addRandomLongColumn(rs: RDD[Rating]): RDD[(Long, Rating)]  = {
//    val rand = new Random()
//    rs.map { r => (rand.nextInt(10).toLong, r) }
//  }

  val addRandomLongColumn: RDD[Rating] => RDD[(Long, Rating)]  = { rs =>
    val rand = new Random()
    rs.map { r => (rand.nextInt(10).toLong, r) }
  }

  val trainingFilter: RDD[(Long, Rating)] => RDD[Rating]  = { rs =>
    rs.filter(x => x._1 <= 3)
      .values
      .repartition(numPartitions)
      .cache
  }
  //val fn: (RDD[(Long, Rating)]) => RDD[Rating] = trainingFilter _

  def validationFilter(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 == 4)
      .values
      .repartition(numPartitions)
      .cache
  }

  def testingFilter(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 == 5)
      .values
      .cache
  }

  def train: (RDD[Rating], RDD[Rating]) =>  MatrixFactorizationModel = {
    (validation, training) =>

    val ranks = List(12)
    val lambdas = List(0.1, 10.0)
    val numIters = List(10)

    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1

    for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
      val model = ALS.train(training, rank, numIter, lambda)

      val validationRmse = computeRmse(model, validation)

      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }
    bestModel.get
  }
}
