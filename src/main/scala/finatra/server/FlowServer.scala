package finatra.server

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.filter.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import finatra.controller.{RecommenderController, DiamondController}
import finatra.module.{RecommenderModule, SparkContextModule}

object FlowServerMain extends FlowServer

class FlowServer extends HttpServer {
  override def modules = Seq(Slf4jBridgeModule, SparkContextModule, RecommenderModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[DiamondController]
      .add[RecommenderController]
  }

  override def warmup() {
  }
}