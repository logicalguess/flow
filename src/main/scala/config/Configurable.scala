package config

import scala.util.Try

/**
  * Created by logicalguess on 4/16/16.
  */
trait Configurable {

  val config: Configuration
}

trait Configuration {

  def getConfig(key: String): Option[Configuration]

  def getConfigPath(key: String): Option[Configuration] = None

  def getString(key: String): Option[String]

  def getString(key: String, default: String): String =
    getString(key) getOrElse default

  def getInt(key: String): Option[Int]

  def getInt(key: String, default: Int): Int =
    getInt(key) getOrElse default

  def getBoolean(key: String) =
    getString(key).get.toBoolean

  def getBoolean(key: String, default: Boolean = false) =
    getString(key).getOrElse(default.toString).toBoolean

//  def get[T](key: String, thenApply: (String => T) = (v: String) => v.asInstanceOf[T]): Option[T] = {
//    //config.getString(key).map(thenApply)
//    getString(key).flatMap(v => Try(thenApply(v)).toOption)
//  }
//
//  def get[T](key: String, thenApply: (String => T) = (x: String) => x.asInstanceOf[T], default: T = null): T = {
//    get[T](key, thenApply) getOrElse default
//  }

  def exists(key: String) = !getString(key).isEmpty


  def getStringList(key: String): List[String]

  def getStringList(key: String, default: List[String]): List[String] = {
    val list = getStringList(key)
    if (list.isEmpty) default else list
  }

  def toMap: Map[String, Any]

  def toStringMap: Map[String, String]
}

