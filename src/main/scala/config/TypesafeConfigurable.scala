package config

/**
  * Created by logicalguess on 4/16/16.
  */

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
import scala.util.Try

trait TypesafeConfigurable extends Configurable {

  val config: Configuration = new TypesafeConfiguration()
}

class TypesafeConfiguration(typeSafeConfig: Option[Config] = None,
                            file: Option[File] = None,
                            resource: Option[String] = None,
                            subPath: Option[String] = None) extends Configuration {

  val conf: Config = {
    val res = file.fold(typeSafeConfig.getOrElse(ConfigFactory.load())) { externalFile =>
      val fileConfig = ConfigFactory.parseFile(externalFile)
      typeSafeConfig.fold(fileConfig)(_.withFallback(fileConfig))
    }

    subPath.fold(
      resource.fold(res)(ConfigFactory.load)
    ) { path => {
      resource.fold(res) { typeSafeResource =>
        typeSafeConfig.fold(ConfigFactory.load(typeSafeResource)) { tConfig =>
          tConfig.withFallback(ConfigFactory.load(typeSafeResource))
        }
      }
    }.getConfig(path)
    }
  }

  def getConfigFromConfig(typeSafeConfig: Config, subPath: Option[String] = None): Option[Configuration] =
    Try {
      new TypesafeConfiguration(Option(typeSafeConfig), file, resource, subPath)
    }.toOption

  def getConfigFromFile(file: File, subPath: Option[String] = None): Option[Configuration] =
    Try {
      new TypesafeConfiguration(None, Option(file), None, subPath)
    }.toOption

  def getConfigFromResource(resource: String, subPath: Option[String] = None): Option[Configuration] =
    Try {
      new TypesafeConfiguration(None, None, Option(resource), subPath)
    }.toOption


  def getConfig(typeSafeConfig: Option[Config],
                resource: Option[String] = None,
                file: Option[File] = None,
                subPath: Option[String] = None): Option[Configuration] =
    Try {
      new TypesafeConfiguration(typeSafeConfig, file, resource, subPath)
    }.toOption

  /**
    * Generic method override from ConfigComponent
    */
  def getConfig(subPath: String): Option[Configuration] =
    Try {
      new TypesafeConfiguration(Option(conf), None, None, Option(subPath))
    }.toOption

  def mergeConfig(typeSafeConfig: Config): Configuration =
    new TypesafeConfiguration(Option(conf.withFallback(typeSafeConfig)))

  def getSubConfig(subConfigKey: String): Option[Configuration] =
    Try {
      new TypesafeConfiguration(Option(conf.getConfig(subConfigKey)))
    }.toOption

  def getString(key: String): Option[String] =
    Try {
      conf.getString(key)
    }.toOption

  def getInt(key: String): Option[Int] =
    Try {
      conf.getInt(key)
    }.toOption

  def getStringList(key: String): List[String] =
    Try {
      conf.getStringList(key).toList
    }.getOrElse(List.empty[String])

  def toMap: Map[String, Any] = conf.root().toMap

  def toStringMap: Map[String, String] =
    conf.entrySet().map(entry => (entry.getKey, conf.getAnyRef(entry.getKey).toString)).toMap[String, String]
}


