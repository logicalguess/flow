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
    val (recommendations, duration, model_duration, feature_duration, rmse, url1, url2, url3) = recSvc.getRecommendationsForUser(request.params("userId").toInt, recCount)
    val results = recSvc.getItems(recommendations.toList.map { r => r.product })
      .zip(recommendations.map {r => r.rating})
      .map(tuple => (MovieView(tuple._1, tuple._2)))

    val view = RecommenderView(results, duration, rmse, url1, url2)

    val header =
      """
        |<!DOCTYPE html>
        |<html lang="en">
        |<head>
        |    <meta charset="UTF-8">
        |    <title>Title</title>
        |    <link rel="stylesheet" type="text/css" href="http://localhost:8888/assets/semantic/semantic.min.css">
        |    <script src="http://localhost:8888/assets/jquery/jquery-1.12.0.min.js"></script>
        |    <script src="http://localhost:8888/assets/semantic/semantic.min.js"></script>
        |</head>
        |<body>
        |<h1>Recommended Movies</h1>
        |<table class="ui very basic collapsing celled table">
        |    <thead>
        |    <tr><th>Movie Title</th>
        |        <th>Predicted Rating</th>
        |    </tr></thead>
        |    <tbody>
      """.stripMargin

    val tableEnd =
      """
        |    </tbody>
        |</table>
      """.stripMargin

    val footer =
      """
        |    </tbody>
        |</table>
        |
        |<script>
        |    $('.ui.rating').rating();
        |</script>
        |</body>
        |</html>
      """.stripMargin

    val row1 =
      """
        |<tr>
        |        <td>
        |            <h4 class="ui image header">
        |                <i class="film icon"></i>
        |                <div class="content">
      """.stripMargin
    val row2 =
      """
        |                    <div class="sub header">
      """.stripMargin
    val row3 =
      """
        |                   </div>
        |                </div>
        |            </h4></td>
        |        <td>
        |            <div class="ui star rating disabled" data-rating="
      """.stripMargin

    val row4 =
      """
        |" data-max-rating="
      """.stripMargin

    val row5 =
      """
        |                   "></div>
        |        </td>
        |    </tr>
      """.stripMargin

    val moviesHtml = view.results.map {
      case MovieView(title, rating) => {
        val (t, y) = tilteAndYear(title)
        row1 + t  + row2 + y + row3 + rating.round + row4 + rating.round + row5
      }
    }.mkString("\n")

    val imgHtml =
      s"""
        |</br>
        |<h1>Execution Pipelines</h1>
        |</br>
        |<div class="ui four cards">
        |  <div class="card">
        |    <div class="image">
        |      <img src="$url3">
        |    </div>
        |    <div class="content">
        |      <div class="header">Feature Pipeline</div>
        |      <div class="meta">
        |        <a>Feature extraction</a>
        |      </div>
        |      <div class="description">
        |        Executed before runtime.
        |      </div>
        |    </div>
        |    <div class="extra content">
        |        <span>
        |        <i class="wait icon"></i>
        |        $feature_duration ms
        |      </span>
        |    </div>
        |  </div>
        |  <div class="card">
        |    <div class="image">
        |      <img src="$url1">
        |    </div>
        |    <div class="content">
        |      <div class="header">Learning Pipeline</div>
        |      <div class="meta">
        |        <a>Model training</a>
        |      </div>
        |      <div class="description">
        |        Executed before runtime.
        |      </div>
        |    </div>
        |    <div class="extra content">
        |    <span class="right floated">
        |                RMSE: $rmse
        |    </span>
        |        <span>
        |        <i class="wait icon"></i>
        |        $model_duration ms
        |      </span>
        |    </div>
        |  </div>
        |          <div class="card">
        |    <div class="image">
        |      <img src="$url2">
        |    </div>
        |    <div class="content">
        |      <div class="header">Predicting Pipeline</div>
        |      <div class="meta">
        |        <span class="date">Uses the model to recommend movies</span>
        |      </div>
        |      <div class="description">
        |        Executed on every recommending request.
        |      </div>
        |    </div>
        |    <div class="extra content">
        |        <span>
        |        <i class="wait icon"></i>
        |        $duration ms
        |      </span>
        |    </div>
        |  </div>
        |</div>
      """.stripMargin


    response.ok.html(header + moviesHtml + tableEnd + imgHtml + footer)

  }

  def tilteAndYear(mixed: String) = {
    val start = mixed.lastIndexOf('(')
    if (start > 1) {
      val stop = mixed.lastIndexOf(')')
      (mixed.substring(0, start), mixed.substring(start + 1, stop))
    }
    else (mixed, "")
  }

}

object Test {
  def main(args: Array[String]) {
//    val mf: MustacheFactory  = new DefaultMustacheFactory();
//    val mustache: Mustache  = mf.compile("recommender.mustache");
//    val view = RecommenderView(List[MovieView](MovieView("titlez", 0.1), MovieView("titley", 0.2)), 0, "", "")
//    mustache.execute(new PrintWriter(System.out), view).flush();
  }
}
