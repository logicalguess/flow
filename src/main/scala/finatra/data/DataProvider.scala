package finatra.data

import dag.DAG
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD

/**
 * Holds ratings and a Map which maps IDs to names of products
 */
trait DataProvider {
  protected val ratings: RDD[Rating]
  protected val productNames: Map[Int, String]
  protected val graph: Option[DAG] = None
  protected val duration: Option[Long] = None

  def getRatings(): RDD[Rating] = ratings
  def getProductNames(): Map[Int, String] = productNames
  def getGraph(): Option[DAG] = graph
  def getDuration(): Option[Long] = duration
}

class WrongInputDataException extends Exception

