package finatra.controller

import javax.inject.Singleton

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

@Singleton
class AssetController extends Controller {
  get("/assets/:type/:file") { request: Request =>
    val file = request.getParam("file")
    val assetType = request.getParam("type")
    response.ok.file(s"$assetType/$file")
  }

  get("/assets/:*") { request: Request =>
    response.ok.fileOrIndex(
      request.params("*"),
      "html/index.html")
  }
}
