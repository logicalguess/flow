package config

import config.ArgumentsParser.Syntax

/**
  * Created by logicalguess on 4/17/16.
  */

trait CLIConfigurable extends Configurable {
  val args: Array[String]
  val options : Array[(String, CLIOption[Any])] = Array.empty[(String, CLIOption[Any])]

  lazy val config: CLIConfiguration = {
    val self = this
    val parser = new ArgumentsParser {
      override val options = self.options
    }
    new CLIConfiguration(parser.parse(args))
  }
}

class CLIConfiguration(parseResult: ParseResult) extends MapConfiguration(parseResult.optionMap) {
  def remainArgs = parseResult.remainArguments
}

case class CLIOption[+T] (description:String = "", required: Boolean = false, defaultValue: Option[T] = None)

case class ParseResult(optionMap : Map[String, String], remainArguments : Array[String])

/**
  * Parse command line arguments
  * Grammar: -option1 value1 -option2 value3 -flag1 -flag2 remainArg1 remainArg2...
  */
trait  ArgumentsParser {

  val ignoreUnknownArgument = false

  def help: Unit = {
    Console.err.println(s"\nHelp: $description")
    var usage = List.empty[String]
    options.map(kv => if(kv._2.required) {
      usage = usage :+ s"-${kv._1} (required:${kv._2.required})${kv._2.description}"
    } else {
      usage = usage :+ s"-${kv._1} (required:${kv._2.required}, default:${kv._2.defaultValue.getOrElse("")})${kv._2.description}"
    })
    usage :+= remainArgs.map(k => s"<$k>").mkString(" ")
    usage.foreach(Console.err.println(_))
  }

  def parse(args: Array[String]): ParseResult = {
    val syntax = Syntax(options, remainArgs, ignoreUnknownArgument)
    ArgumentsParser.parse(syntax, args)
  }

  val description: String = ""
  val options : Array[(String, CLIOption[Any])] = Array.empty[(String, CLIOption[Any])]
  val remainArgs : Array[String] = Array.empty[String]
}

object ArgumentsParser {

  case class Syntax(options: Array[(String, CLIOption[Any])], remainArgs : Array[String], ignoreUnknownArgument: Boolean)

  def parse(syntax: Syntax, args: Array[String]): ParseResult = {
    import syntax.{ignoreUnknownArgument, options, remainArgs}
    var config = Map.empty[String, String]
    var remain = Array.empty[String]

    def doParse(argument: List[String]): Unit = {
      argument match {
        case Nil => Unit // true if everything processed successfully

        case key :: value :: rest if key.startsWith("-") && !value.startsWith("-") =>
          val fixedKey = key.substring(1)
          if (!options.map(_._1).contains(fixedKey)) {
            if (!ignoreUnknownArgument) {
              throw new Exception(s"found unknown option $fixedKey")
            } else {
              remain ++= Array(key, value)
            }
          } else {
            config += fixedKey -> value
          }
          doParse(rest)

        case key :: rest if key.startsWith("-") =>
          val fixedKey = key.substring(1)
          if (!options.map(_._1).contains(fixedKey)) {
            throw new Exception(s"found unknown option $fixedKey")
          } else {
            config += fixedKey -> "true"
          }
          doParse(rest)

        case value :: rest =>
          Console.err.println(s"Warning: get unknown argument $value, maybe it is a main class")
          remain ++= value :: rest
          doParse(Nil)
      }
    }
    doParse(args.toList)

    options.foreach{pair =>
      val (key, option) = pair
      if (!config.contains(key) && !option.required) {
        config += key -> option.defaultValue.getOrElse("").toString
      }
    }

    options.foreach { pair =>
      val (key, value) = pair
      if (config.get(key).isEmpty) {
        throw new Exception(s"Missing option ${key}...")
      }
    }

    if (remain.length < remainArgs.length) {
      throw new Exception(s"Missing arguments ...")
    }

    new ParseResult(config, remain)
  }
}
