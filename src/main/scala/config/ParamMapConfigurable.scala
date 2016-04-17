package config

/**
  * Created by logicalguess on 4/16/16.
  */

import org.apache.spark.ml.param.ParamMap

import scala.util.Try

trait ParamMapConfigurable extends Configurable {

  val paramMap: ParamMap

  lazy val config: Configuration = new ParamMapConfiguration(paramMap)
}

case class ParamMapConfiguration(paramMap: ParamMap = ParamMap.empty) extends Configuration {
  implicit def paramMapConfigurationToParamMap(pmc: ParamMapConfiguration): ParamMap = pmc.paramMap

  val conf: Map[String, Any] = paramMap.toSeq.map(pair => pair.param.name -> pair.value).toMap

  def getConfig(key: String): Option[Configuration] =
    Try {
      new ParamMapConfiguration(conf.get(key).asInstanceOf[ParamMap])
    }.toOption

  def getString(key: String): Option[String] =
    conf.get(key).flatMap(v => Try(v.toString).toOption)

  def getInt(key: String): Option[Int] =
    conf.get(key).flatMap(v => Try(v.toString.toInt).toOption)

  private val listRegex = """\[(.*)\]""".r

  def getStringList(key: String): List[String] =
    getString(key).fold {
      List.empty[String]
    } {
      case listRegex(listValues) => listValues.split(",").toList
      case v => List.empty[String]
    }

  def toMap: Map[String, Any] = conf

  def toStringMap: Map[String, String] = conf.map(entry => (entry._1, entry._2.toString))
}


