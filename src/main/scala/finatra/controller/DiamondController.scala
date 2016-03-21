package finatra.controller

import javax.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import finatra.service.DiamondService

@Singleton
class DiamondController @Inject()(service: DiamondService) extends Controller {

  get("/diamond/:start") { request: Request =>
    service.runDiamond(request.params("start").toInt)
  }
}
