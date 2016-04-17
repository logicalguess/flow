package config

/**
  * Created by logicalguess on 4/17/16.
  */
import org.scalatest.{FlatSpec, Matchers}

class CLIConfigSpec extends FlatSpec with Matchers {

  it should "parse arguments correctly" in {

    trait SimpleCLIConfig extends CLIConfigurable {
      override val options = Array(
        "flag" -> CLIOption[Any]("", required = true),
        "opt1" -> CLIOption[Any]("", required = true),
        "opt2" -> CLIOption[Any]("", required = true))
    }

    val configurable = new SimpleCLIConfig {
      override val args = Array("-flag" , "-opt1", "1","-opt2", "2", "arg1", "arg2")
    }

    val config = configurable.config

    assert(config.getBoolean("flag"))
    assert(config.getInt("opt1").get == 1)
    assert(config.getString("opt1").get == "1")
    assert(config.getInt("opt2").get == 2)

    assert(config.remainArgs(0) == "arg1")
    assert(config.remainArgs(1) == "arg2")
  }

  it should "handle interleaved options and remain args" in {

    val configurable = new CLIConfigurable {
      override val args: Array[String] = Array("-opt1", "1","xx.MainClass", "-opt2", "2")
      override val options = Array(
        "opt1" -> CLIOption[Any]("", required = true))
    }

    assert(configurable.config.getInt("opt1").get == 1)
    assert(configurable.config.remainArgs.length == 3)
  }
}



