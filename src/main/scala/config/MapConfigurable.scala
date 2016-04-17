package config

/**
  * Created by logicalguess on 4/16/16.
  */

import scala.util.Try

trait MapConfigurable extends Configurable {

  val memoryMap: Map[String, Any]

  lazy val config: Configuration = new MapConfiguration(memoryMap)
}

class MapConfiguration(memoryMap: Map[String, Any], subPath: Option[String] = None) extends Configuration {

  val conf: Map[String, Any] =
    subPath match {
      case Some(key) =>
        memoryMap.get(key) match {
          case Some(map: Map[String, Any]@unchecked) => map
          case _ => throw new Exception(s"Path $subPath not found.")
        }
      case None => memoryMap
    }

  def getConfig(key: String): Option[Configuration] =
    Try {
      new MapConfiguration(memoryMap, Option(key))
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

