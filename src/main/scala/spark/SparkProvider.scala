package spark

import flow.Operation
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by logicalguess on 3/12/16.
  */

sealed trait SparkProviderType
case object LOCAL extends SparkProviderType
case object SHARED extends SparkProviderType

case class SparkProvider(appName: String) {
  def apply(sparkProviderType: SparkProviderType): Operation[SparkContext] = {
    sparkProviderType match {
      case LOCAL =>
        Operation[SparkContext] {
          val sparkConfig = new SparkConf()
            .setAppName(appName)
            .setMaster("local[2]")
          new SparkContext(sparkConfig)
        }
      case _ => throw new IllegalArgumentException("unsupported spark provider type")
    }
  }
}

