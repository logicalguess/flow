package finatra.controller

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.Controller
import finatra.service.RecommenderService

@Singleton
class RecommenderController @Inject()(recSvc: RecommenderService, @Flag("rec.count") recCount: Int) extends Controller {

  get("/recommender/:userId") { request: Request =>
    val (recommendations, rmse, url1, url2) = recSvc.getRecommendationsForUser(request.params("userId").toInt, recCount)
    val results = recSvc.getItems(recommendations.toList.map { r => r.product })
      .zip(recommendations.map {r => r.rating})
      .map(tuple => (Map("title" -> tuple._1, "rating" -> tuple._2)))
    Map("recommendations" -> results, "rmse" -> rmse, "url1" -> url1, "url2" -> url2)
  }

}
