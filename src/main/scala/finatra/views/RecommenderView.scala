package finatra.views

import com.twitter.finatra.response.Mustache

/**
  * Created by logicalguess on 3/21/16.
  */

@Mustache("recommender")
case class RecommenderView(results: List[MovieView], duration: Long, rmse: Double, url1:String, url2: String, markup: String = "")

case class MovieView(title: String, rating: Double)
