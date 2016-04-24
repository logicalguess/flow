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
import util.Timing

/**
  * Created by logicalguess on 2/26/16.
  */

@Singleton
case class FlowALSRecommenderService @Inject()(sc: SparkContext, dataProvider: DataProvider) extends RecommenderService {

  val modelExecution: ExecutionInfo = createModel()

  val model: MatrixFactorizationModel = modelExecution.result.get.asInstanceOf[MatrixFactorizationModel]

  def getRecommendationsForUser(userId: Int, count: Int) = {

    val userIdFn = {userId}
    val sparkContextFn = {sc}
    val modelFn = {model}
    val productsFn = dataProvider.getProductNames

    val candidatesFn: (SparkContext, Map[Int, String]) => RDD[Int] = {
      (sc: SparkContext, products: Map[Int, String]) => sc.parallelize(products.keys.toSeq).cache()
    }

    val mapByIdFn: (Int, RDD[Int]) => RDD[(Int, Int)] =  {
      (userId: Int, rdd: RDD[Int]) => rdd.map((userId, _)).cache()
    }
    val predictFn: (MatrixFactorizationModel, RDD[(Int, Int)]) => Array[Rating] = {
      (model,  rdd: RDD[(Int, Int)]) =>
        model
        .predict(rdd)
        .collect
        .sortBy(- _.rating)
        .take(count)
    }
    //val fns = predictFn(mapByIdFn(userId, candidatesFn(sparkContextFn, productsFn)))

    val graph = DAG("flow",
      List("userId"),
      List("spark_context"),
      List("movie_names"),
      List("model"),
      List("candidates", "spark_context", "movie_names"),
      List("mapped_by_id", "userId", "candidates"),
      List("predict", "model", "mapped_by_id"))

    val ops = OperationBuilder(graph,
      Map("spark_context" -> sparkContextFn,
        "userId" -> userIdFn,
        "movie_names" -> productsFn,
        "model" -> modelFn),
      Map("candidates" -> candidatesFn,
        "mapped_by_id" -> mapByIdFn,
        "predict" -> predictFn))

    val (recs: List[Rating], predict_duration: Long) = Timing.time {ops("predict")().asInstanceOf[Array[Rating]].toList }

    List(
      ExecutionInfo("predict", Some(recs), predict_duration, Some(graph)),
      modelExecution.noResult,
      ExecutionInfo("feature", None, dataProvider.getDuration().getOrElse(0), dataProvider.getGraph())
    )
//    val predict_url = Util.gravizoDotLink(DAG.dotFormatDiagram(graph, true))
//    val data_url = dataProvider.getGraph().map(g => Util.gravizoDotLink(DAG.dotFormatDiagram(g))).getOrElse("")
//    val data_duration: Long = dataProvider.getDuration().getOrElse(0)

//    (recs, predict_duration, model_duration,
//      data_duration,"%.3f".format(rmse).toDouble, model_url,
//      predict_url, data_url)
  }

  def createModel(): ExecutionInfo = {

    val ratingsFn: RDD[Rating] = dataProvider.getRatings

//    val ratings = addRandomLongColumnFn(ratingsFn)
//    val model = train(trainingFilterFn(ratings), validationFilterFn(ratings))
//    val testRmse = computeRmse(model, testingFilterFn(ratings))
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

    val (m: MatrixFactorizationModel, duration: Long) = Timing.time {ops("model")().asInstanceOf[MatrixFactorizationModel]}

    ExecutionInfo("model", Some(m), duration, Some(graph),
      Some("%.3f".format(ops("rmse")().asInstanceOf[Double]).toDouble))

//    (m, duration, ops("rmse")().asInstanceOf[Double],
//      Util.gravizoDotLink(DAG.dotFormatDiagram(graph, true)))
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


//  def addRandomLongColumnFn(rs: RDD[Rating]): RDD[(Long, Rating)]  = {
//    val rand = new Random()
//    rs.map { r => (rand.nextInt(10).toLong, r) }
//  }

  val addRandomLongColumnFn: RDD[Rating] => RDD[(Long, Rating)]  = { rs =>
    val rand = new Random()
    rs.map { r => (rand.nextInt(10).toLong, r) }
  }

  val trainingFilterFn: RDD[(Long, Rating)] => RDD[Rating]  = { rs =>
    rs.filter(x => x._1 <= 3)
      .values
      .repartition(numPartitions)
      .cache
  }
  //val fn: (RDD[(Long, Rating)]) => RDD[Rating] = trainingFilterFn _

  def validationFilterFn(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 == 4)
      .values
      .repartition(numPartitions)
      .cache
  }

  def testingFilterFn(rs: RDD[(Long, Rating)]): RDD[Rating]  = {
    rs.filter(x => x._1 == 5)
      .values
      .cache
  }

  def train: (RDD[Rating], RDD[Rating]) =>  MatrixFactorizationModel = { (validation, training) =>
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
