package finatra.views

import com.twitter.finatra.response.Mustache

@Mustache("flow")
case class FlowResult(result: Any, imgSrc: String)