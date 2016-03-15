#Simple example
        val transformerIntToString = Transformer[Int, String] { i: Int => i.toString }
        val transformerAppendBang = Transformer[String, String] { s: String => s + "!" }
        val transformerAppendHash = Transformer[String, String] { s: String => s + "#" }
        val transformerConcatenate = Transformer[(String, String), String] { s: (String, String) => s._1 + s._2 }
        
        
        
            "diamond" in new TryExecutor {
              def flow(start: Int) = {
                for {
                  start <- Provider[Int](start)
                  s1 <- transformerIntToString(start)
                  s2 <- transformerAppendBang(s1)
                  s3 <- transformerAppendHash(s1)
                  s4 <- transformerConcatenate(s2, s3)
                } yield s4
              }
        
              execute(flow(7)) shouldBe Success("7!7#")
              
              
#Implicits (functions as operations)
             
  "Implicit examples" should {

    val transformerIntToString : Transformer[Int, String] = { i: Int => i.toString }
    val transformerAppendBang = { s: String => s + "!" }
    val transformerAppendHash = { s: String => s + "#" }
    val transformerConcatenate = { s: (String, String) => s._1 + s._2 }

    "linear" in new DummyExecutor {
      val result = for {
        s <- transformerIntToString(3)
        ss <- transformerAppendBang(s)
      } yield ss

      execute(result) shouldBe "3!"
    }
  }
                
#Spark examples             
        
  "Spark examples" should {

    val scStart: Operation[SparkContext] = SparkProvider("test")(LOCAL)

    val scStop = Transformer[SparkContext, Unit] { _.stop() }


    val wordsRDD: SparkOperation[RDD[String]] = SparkOperation[RDD[String]] { sc =>
      sc.makeRDD("There is nothing either good or bad, but thinking makes it so".split(' '))
    }

    val aWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("a")) }
    val bWords = RDDTransformer[String, String] { rdd => rdd.filter(_.contains("b")) }

    val countOperation = Transformer[RDD[String], Long] { _.count }

    "words" in new TryExecutor {
      val operation = scStart --> wordsRDD // same as wordsRDD(sc)
      val result: Try[RDD[String]] = execute(operation)
      result.get.count() shouldBe 12
    }

    "letter count" in new FutureExecutor {
      import scala.concurrent.ExecutionContext.Implicits.global

      val flow = for {
        sc <- scStart
        words <- wordsRDD(sc)

        aWords <- aWords(words)
        countA <- countOperation(aWords)

        bWords <- bWords(words)
        countB <- countOperation(bWords)

        //_ <- scStop(sc)
      }
        yield (countA, countB)

      val futureResult = execute(flow)

      whenReady(futureResult) { result =>
        scStart().stop()
        result shouldBe (2, 2)
      }
    }
  }
  
#Free Monads

    class OpSuite extends WordSpec with ShouldMatchers with Logging with ScalaFutures {
    
      val f_str = { i: Int => i.toString }
      val f_bang = { s: String => s + "!" }
      val f_hash = { s: String => s + "#" }
      val f_concat = { s: (String, String) => s._1 + s._2 }
    
      "function examples" should {
    
        val logic: String = {
          val i = 5
          val s = f_str(i)
          val b = f_bang(s)
          val h = f_hash(s)
          f_concat(b, h)
        }
    
        "composition" in {
          logic shouldBe "5!5#"
        }
    
      }
    
      "op examples" should {
    
        val op = Op(5)
        val str: Tr[Int, String] = f_str
        val bang: Tr[String, String] = f_bang
        val hash: Tr[String, String] = f_hash
        val concat: Tr[(String, String), String] = f_concat
    
        val logic: Free[External, String] = for {
          i <- op
          s <- str(i)
          b <- bang(s)
          h <- hash(s)
          c <- concat(b, h)
        } yield c
    
        "id" in {
    
          val result = logic.foldMap(idInterpreter)
    
          result shouldBe "5!5#"
        }
    
        "future" in {
    
          val futureResult = logic.foldMap(futureInterpreter)
    
          whenReady(futureResult) { result =>
            result shouldBe "5!5#"
          }
        }
    
        "log" in {
          logic.foldMap(logInterpreter)
        }
      }
    }