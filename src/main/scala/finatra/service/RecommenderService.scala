package finatra.service

import dag.{DAG, Util}
import finatra.data.DataProvider

/**
  * Created by logicalguess on 2/28/16.
  */
trait RecommenderService {
  def dataProvider: DataProvider

  def getRecommendationsForUser(userId: Int, count: Int): Seq[ExecutionInfo]

  def getItems(itemIds: List[Int]): List[String] = {
    val products = dataProvider.getProductNames()
    itemIds.map { id => products(id)}
  }
}

case class ExecutionInfo(name: String, result: Option[Any], duration: Long = -1,
                            graph: Option[DAG] = None, extra: Option[Any] = None) {
  def noResult = ExecutionInfo(name, None, duration, graph, extra)
  def graphURL() = graph.map(g => Util.gravizoDotLink(DAG.dotFormatDiagram(g, true))).getOrElse("")
}
