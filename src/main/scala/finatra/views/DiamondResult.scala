package finatra.views

import com.twitter.finatra.response.Mustache

@Mustache("diamond")
case class DiamondResult(result: Any, imgSrc: String)