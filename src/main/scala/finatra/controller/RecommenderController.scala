package finatra.controller

import java.io.PrintWriter
import javax.inject.Singleton

import com.github.mustachejava.{Mustache, DefaultMustacheFactory, MustacheFactory}
import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.Controller
import finatra.service.RecommenderService
import finatra.views.{MovieView, RecommenderView}

@Singleton
class RecommenderController @Inject()(recSvc: RecommenderService, @Flag("rec.count") recCount: Int) extends Controller {

  get("/recommender/:userId") { request: Request =>
    val (recommendations, rmse, url1, url2) = recSvc.getRecommendationsForUser(request.params("userId").toInt, recCount)
    val results = recSvc.getItems(recommendations.toList.map { r => r.product })
      .zip(recommendations.map {r => r.rating})
      .map(tuple => (MovieView(tuple._1, tuple._2)))
    val view = RecommenderView(results, rmse, url1, url2)

    val resultHtml = "<h2>Recommended movies:</h2>\n"
    val moviesHtml = view.results.map {
      case MovieView(title, rating) => "<li>" + title + " -- " + rating + "</li>"
    }.mkString("\n")
    val imgHtml1 = "<br><img src=" + view.url1 + " width=\"250\"/>\n"
    val imgHtml2 = "<img src=" + view.url2 + " width=\"250\"/>\n"

    response.ok.html(resultHtml + moviesHtml + imgHtml1 + imgHtml2)
  }

}

object Test {
  def main(args: Array[String]) {
    val mf: MustacheFactory  = new DefaultMustacheFactory();
    val mustache: Mustache  = mf.compile("recommender.mustache");
    val view = RecommenderView(List[MovieView](MovieView("titlez", 0.1), MovieView("titley", 0.2)), 0, "", "")
    mustache.execute(new PrintWriter(System.out), view).flush();

  }
}
