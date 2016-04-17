package config

/**
  * Created by logicalguess on 4/17/16.
  */
import com.typesafe.config.ConfigFactory
import org.apache.spark.ml.param.{Param, ParamMap}
import org.scalatest.{FlatSpec, Matchers}

class TypesafeConfigSpec extends FlatSpec with Matchers {

  it should "parse params correctly" in {

    val configText =
      """
        |pipeline {
        |  cpu.interval = 50
        |  memory.interval = 20
        |  processors = 1
        |  persistors = 1
        |}
        |hbase {
        |  table {
        |    name = "pipeline"
        |    column {
        |      family = "metrics"
        |      name = "average"
        |    }
        |  }
        |}
      """.
        stripMargin

//    config/test.conf:
//      flag = true
//      opt1 = 1
//      opt2 = 2
//
//      hbase {
//        table {
//          column {
//            family = "override metrics"
//          }
//        }
//      }

    val configurable = new TypesafeConfigurable {
      override val typeSafeConfig = ConfigFactory.parseString(configText) //overrides the default applicaton.conf
      override val resource = "config/test.conf" //overrides the values in the above
      override val stringConfig =               //overrides the values in the above
        """
          |opt1 = 11
          |opt3 = 3
          |
        """.stripMargin
    }

    val config = configurable.config

    assert(config.getBoolean("flag"))
    assert(config.getInt("opt1").get == 11)
    assert(config.getString("opt1").get == "11")
    assert(config.getInt("opt2").get == 2)
    assert(config.getString("opt3").get == "3")


    assert(config.getInt("pipeline.cpu.interval").get == 50)
    assert(config.getString("hbase.table.column.name").get == "average")
    assert(config.getString("hbase.table.column.family").get == "override metrics")


  }
}



