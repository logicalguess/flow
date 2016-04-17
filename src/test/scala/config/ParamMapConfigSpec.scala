package config

/**
  * Created by logicalguess on 4/17/16.
  */
import org.apache.spark.ml.param.{Param, ParamMap}
import org.scalatest.{FlatSpec, Matchers}

class ParamMapConfigSpec extends FlatSpec with Matchers {

  it should "parse params correctly" in {

    val pm: ParamMap = ParamMap .empty
    pm.put(new Param("", "flag", "") -> true)
    pm.put(new Param("", "opt1", "") -> 1)
    pm.put(new Param("", "opt2", "") -> 2)

    val configurable = new ParamMapConfigurable {
      override val paramMap = pm
    }

    val config = configurable.config

    assert(config.getBoolean("flag"))
    assert(config.getInt("opt1").get == 1)
    assert(config.getString("opt1").get == "1")
    assert(config.getInt("opt2").get == 2)
  }
}



