package finatra.service

import javax.inject.{Inject, Singleton}

import dag.{DAG, Util}
import finatra.data.DataProvider
import flow.OperationBuilder
import util.FunctionImplicits._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

import scala.util.Random

import Functions._

/**
  * Created by logicalguess on 2/26/16.
  */

@Singleton
case class FlowALSRecommenderService @Inject()(sc: SparkContext, dataProvider: DataProvider) extends RecommenderService {

  val (model, rmse, url) = createModel()

  def getRecommendationsForUser(userId: Int, count: Int) = {

    val userIdFn = {userId}
    val sparkContextFn = {sc}
    val modelFn = {model}
    val productsFn = dataProvider.getProductNames
    val candidatesFn: ((SparkContext, Map[Int, String])) => RDD[Int] = t => t match {
      case (sc: SparkContext, products: Map[Int, String]) => sc.parallelize(products.keys.toSeq)
    }
    val mapByIdFn: ((Int, RDD[Int])) => RDD[(Int, Int)] =  t => t match {
      case (userId: Int, rdd: RDD[Int]) => rdd.map((userId, _))
    }
    val predictFn: ((MatrixFactorizationModel, RDD[(Int, Int)])) => Array[Rating] = t => t match {
      case (model,  rdd: RDD[(Int, Int)]) =>
        model
        .predict(rdd)
        .collect
        .sortBy(- _.rating)
        .take(count)
    }
    //val fns = predictFn(mapByIdFn(userId, candidatesFn(sparkContextFn, productsFn)))

    val graph = DAG("flow",
      List("userId"),
      List("sc"),
      List("products"),
      List("model"),
      List("candidates", "sc", "products"),
      List("mapById", "userId", "candidates"),
      List("predict", "model", "mapById"))

    val ops = OperationBuilder(graph,
      Map("sc" -> sparkContextFn,
        "userId" -> userIdFn,
        "products" -> productsFn,
        "model" -> modelFn),
      Map("candidates" -> candidatesFn,
        "mapById" -> mapByIdFn,
        "predict" -> predictFn))

    (ops("predict")().asInstanceOf[Array[Rating]].toList, rmse,
      Util.gravizoDotLink(DAG.dotFormatDiagram(graph)), url)

  }

  def createModel(): (MatrixFactorizationModel, Double, String) = {

    val ratingsFn: RDD[Rating] = dataProvider.getRatings

//    val ratings = addRandomLongColumnFn(ratingsFn)
//    val model = train(trainingFilterFn(ratings), validationFilterFn(ratings))
//    val testRmse = computeRmse(model, testingFilterFn(ratings))
//    (model, testRmse)

    val graph = DAG("recommender",
      List("data"),
      List("ratings", "data"),
      List("training", "ratings"),
      List("validation", "ratings"),
      List("testing", "ratings"),
      List("model", "training", "validation"),
      List("rmse", "model", "testing"))


    val ops = OperationBuilder(graph,
      Map("data" -> ratingsFn),
      Map(
        "ratings" -> addRandomLongColumnFn _,
        "training" -> trainingFilterFn _,
        "validation" -> validationFilterFn _,
        "testing" -> testingFilterFn _,
        "model" -> (train _).tupled,
        "rmse" -> (computeRmse _).tupled
        )
      )

    (ops("model")().asInstanceOf[MatrixFactorizationModel], ops("rmse")().asInstanceOf[Double],
      Util.gravizoDotLink(DAG.dotFormatDiagram(graph)))
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


  def addRandomLongColumnFn(rs: RDD[Rating]): RDD[(Long, Rating)]  = {
    val rand = new Random()
    rs.map { r => (rand.nextInt(10).toLong, r) }
  }

  def trainingFilterFn(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 <= 3)
      .values
      .repartition(numPartitions)
      .persist
  }
  //val fn: (RDD[(Long, Rating)]) => RDD[Rating] = trainingFilterFn _

  def validationFilterFn(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 == 4)
      .values
      .repartition(numPartitions)
      .persist
  }

  def testingFilterFn(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 == 5)
      .values
      .persist
  }

  def train(training: RDD[Rating], validation: RDD[Rating]): MatrixFactorizationModel = {
    val ranks = List(12)
    val lambdas = List(0.1, 10.0)
    val numIters = List(10)

    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1

    val numValidation = validation.count

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
