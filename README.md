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

    "letter count" in new TryExecutor {
      val flow = for {
        sc <- scStart
        words <- wordsRDD(sc)

        aWords <- aWords(words)
        countA <- countOperation(aWords)

        bWords <- bWords(words)
        countB <- countOperation(bWords)

        _ <- scStop(sc)
      }
        yield (countA, countB)

      execute(flow) shouldBe Success((2, 2))
    }
  }