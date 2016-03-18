package finatra.controller

import java.io.File
import javax.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import dag.{Connector, Node, Util, DAG}
import finatra.service.FlowService
import finatra.views.FlowResult
import flow.OperationBuilder

@Singleton
class FlowController @Inject()(flowService: FlowService) extends Controller {

  get("/:start") { request: Request =>

    val view: FlowResult = flowService.runDiamond(request.params("start").toInt)

    val resultHtml = "<h2>Result: " + view.result + "</h2>\n"
    val imgHtml = "<div>\n    <img src=" + view.imgSrc + " width=\"250\"/>\n</div>"

    response.ok.html(resultHtml + imgHtml)
  }

}