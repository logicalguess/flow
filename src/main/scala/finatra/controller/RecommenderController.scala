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
        |<h2>Recommended Movies</h2>
        |<table class="ui very basic collapsing celled table">
        |    <thead>
        |    <tr><th>Movie Title</th>
        |        <th>Expected Rating</th>
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
      case MovieView(title, rating) => row1 + title  + row2 + rating + row3 + rating.round + row4 + rating.round + row5
    }.mkString("\n")

    val imgHtml = "<br><br></div><div><img src=" + view.url1 + " width=\"250\"/>\n" +
      "<img src=" + view.url2 + " width=\"250\"/></div>\n"


    response.ok.html(header + moviesHtml + tableEnd + imgHtml + footer)

  }

}

object Test {
  def main(args: Array[String]) {
//    val mf: MustacheFactory  = new DefaultMustacheFactory();
//    val mustache: Mustache  = mf.compile("recommender.mustache");
//    val view = RecommenderView(List[MovieView](MovieView("titlez", 0.1), MovieView("titley", 0.2)), 0, "", "")
//    mustache.execute(new PrintWriter(System.out), view).flush();
//    val wrapped = "\\(.*?)((.*?)\\)".r
//    val wrapped(r) = "abc (12)"
//    println(r)

  }
}
