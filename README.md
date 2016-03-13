
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