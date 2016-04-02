package flow

import flow.OperationImplicits._
import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

//import scalaz.syntax.bind._

class OperationSuite extends WordSpec with ShouldMatchers with Logging {

  val f_str = { i: Int => i.toString }
  val f_bang = { s: String => s + "!" }
  val f_hash = { s: String => s + "#" }
  val f_concat = { s: (String, String) => s._1 + s._2 }

  "function examples" should {

    val logic: String = {
      val i = 7
      val s = f_str(i)
      val b = f_bang(s)
      val h = f_hash(s)
      f_concat(b, h)
    }

    "composition" in {
      logic shouldBe "7!7#"
    }
  }

  "Implicit examples" should {

    val constant: Transformer[Unit, Int] = { _: Unit => 3}
    val transformerIntToString: Transformer[Int, String] = f_str
    val transformerAppendBang = f_bang
    val transformerAppendHash = f_hash
    val transformerConcatenate: Transformer[(String, String), String] = f_concat

    "linear" in {
      val result = for {
        c <- constant()
        s <- transformerIntToString(c)
        ss <- transformerAppendBang(s)
      } yield ss

      result() shouldBe "3!"
    }
  }

  "Transformer examples" should {

    val constant = Transformer[Unit, Int] { _ => 3}
    val transformerIntToString = Transformer[Int, String] { i: Int => i.toString }
    val transformerAppendBang = Transformer[String, String] { s: String => s + "!" }
    val transformerAppendHash = Transformer[String, String] { s: String => s + "#" }
    val transformerConcatenate = Transformer[(String, String), String] { s: (String, String) => s._1 + s._2 }

    "linear" in {

      val result = for {
        c <- constant()
        s <- transformerIntToString(c)
        ss <- transformerAppendBang(s)
      } yield ss

      result() shouldBe "3!"

    }

    "diamond" in {
      def flow(start: Int) = {
        for {
          startOp <- Operation[Int](start)
          s1 <- transformerIntToString(startOp)
          s2 <- transformerAppendBang(s1)
          s3 <- transformerAppendHash(s1)
          s4 <- transformerConcatenate(s2, s3)
        } yield s4
      }

      flow(7)() shouldBe "7!7#"
    }
  }

  "Case Class examples" should {

    case class IntToString(i: Int) extends Operation[String] {
      def apply = { i.toString }
    }

    case class AppendBang(s: String) extends Operation[String] {
      def apply = { s + "!" }
    }

    case class AppendHash(s: String) extends Operation[String] {
      def apply = { s + "#" }
    }

    case class Concat(s1: String, s2: String) extends Operation[String] {
      def apply = { s1 + s2 }
    }

//    val transfConcat = Transformer[(String, String), String] { s =>
//      s match {
//        case (s1, s2) => s1 + s2
//      }
//    }

    "linear" in {

      val result = for {
        s <- IntToString(3)
        ss <- AppendBang(s)
      } yield ss

      result() shouldBe "3!"
    }

    "diamond" in {
      def flow(start: Int) = {
        for {
          startOp <- Operation[Int](start)
          s1 <- IntToString(startOp)
          s2 <- AppendBang(s1)
          s3 <- AppendHash(s1)
          s4 <- Concat(s2, s3) //Operation(Concat(s2, s3).apply) //transfConcat(s2, s3)
        } yield s4
      }

      flow(7)() shouldBe "7!7#"
    }
  }

  "Simple examples" should {
    "operation as function" in {
      val operationInt = Operation {
        123
      }
      val operationString = Operation {
        "mama"
      }

      val operationWithDependencies =
        (first: Int, second: String) ⇒ Operation {
          first.toString + second
        }

      val merged = for {
        i ← operationInt
        s ← operationString
        together ← operationWithDependencies(i, s)
      } yield together

      merged() shouldBe "123mama"
    }

    "operation as class" in {
      val operationInt = Operation {
        123
      }
      val operationString = Operation {
        "mama"
      }

      case class SomeMergedGuy(first: Int, second: String) {
        def apply() = Operation {
          first.toString + second
        }
      }

      val merged = for {
        i ← operationInt
        s ← operationString
        together ← SomeMergedGuy(i, s)()
      } yield together

      merged() shouldBe "123mama"
    }

    "operation as wrapper" in {
      class WrappedOperation {
        def op1 = Operation {
          123
        }

        def apply() = op1.map(_ * 2)
      }

      val operationInt = (new WrappedOperation) ()
      operationInt() shouldBe 246
    }

    "operation as function instance" in {
      class FunctionOperation extends (() ⇒ Operation[Int]) {
        def apply() = Operation {
          123
        }
      }

      val operationInt = new FunctionOperation
      operationInt()() shouldBe 123
    }

    "longer computation" in {
      case class Provider1(first: Int, second: Int) {
        def apply() = Operation {
          first + second
        }
      }

      case class Provider2(first: String, second: String) {
        def apply() = Operation {
          first.length + second.length
        }
      }

      val myProvider = for {
        r1 ← Provider1(1, 1)()
        r2 ← Provider2("a", "bcd")()
        summed ← Provider1(r1, r2)()
        // these could go inside yield
        a = 123
        b = 23
      } yield a - b + summed

      myProvider() shouldBe (100 + 2 + 4)
    }

    "simple fill" in {
      case class Provider(first: Int, second: Int) {
        def apply() = Operation {
          first + second
        }
      }

      val quiteFilled = (second: Int) ⇒ for {
        p ← Provider(500, second)()
      } yield p

      quiteFilled(250)() shouldBe 750
    }

    "bit more complex fill" in {
      case class Provider(first: Int, second: Int) {
        def apply() = Operation {
          first + second
        }
      }

      val harder = (first: Int, second: Int) ⇒ for {
        p1 ← Provider(500, second)()
        p2 ← Provider(first, 100)()
      } yield p1 + p2

      harder(-500, -100)() shouldBe 0

      val another = for {
        x ← harder(-500, -100)
        y ← Provider(3, 7)()
      } yield x + y

      another() shouldBe 10
    }

    "generic operation should compile" in {
      case class Generic[A]() {
        def apply() = Operation {
          555
        }
      }

      val z = new Generic[Int]()
      z().map(_.toString)
      z().flatMap(null)

      for {x ← Generic[Int]()()} yield ()
    }
  }

  "Advanced examples" should {
    "Partial graph fill" in {
      val IProduceInt = Operation {
        666
      }

      case class ITake3Arguments(first: Int, second: Boolean, third: String) {
        def apply() = Operation {
          first.toString + second.toString + third.toString
        }
      }

      // also could be another case class
      val firstHoleFilled = (second: Boolean) ⇒ for {
        i ← IProduceInt
        m ← ITake3Arguments(i, second, "mama")()
      } yield m

      firstHoleFilled(false)() shouldBe "666falsemama"
    }

    "Mutating operation (prehaps for that dynamic case on whiteboard?)" in {
      val IProduceInt = Operation {
        10
      }

      import scala.collection.mutable._
      class DynamicOperation(startSum: Int, xs: Buffer[String]) {
        def apply() = Operation {
          xs.foldRight(startSum)(_.length + _) // or I do some join on some other tables or something
        }
      }

      val xs = Buffer.empty[String]
      val operation = new DynamicOperation(0, xs)

      val dynamicOperation = for {
        i ← IProduceInt
        r ← (new DynamicOperation(i, xs)) ()
      } yield r

      dynamicOperation() shouldBe 10 + 0
      xs += "test"
      //dynamicOperation() shouldBe 10 + 4
    }

  }

  "Tutorials" should {
    // if you miss/don't know something either come to me (marek kadek)
    // or feel free to expand.

    // Monad allows us to chain computation.
    // - you can use value from operation1 in operation2, if it was needed, i.e. ...b <- operation2(a)
    // - first problem stops the computation. i.e. if operation2 died (exception), operation3 would not apply
    // - hence is not parallelizable (because next steps may depend on previous)
    "Monad" in {
      val operation1 = Operation {
        1
      }
      val operation2 = Operation {
        2
      }
      val operation3 = Operation {
        3
      }

      // we don't use next stuff we from previous computations... it's so that I can show same example with
      // applicative
      val sumUp = for {
        a ← operation1
        b ← operation2
        c ← operation3
      } yield a + b + c

      sumUp() shouldBe (1 + 2 + 3)
    }

    // Applicatives are less common but just as useful as monads.
    // - the operations are independent
    // - all computations apply, i.e. if operation2 dies (exception), operation 1 and 3 would apply
    // - hence is parallelizable - there is no dependency on context (no `previous step`)
    //    "Applicative" in  {
    //      val operation1 = Operation { 1 }
    //      val operation2 = Operation { 2 }
    //      val operation3 = Operation { 3 }
    //
    //      val sumUp = Operation.monad.apply3(operation1, operation2, operation3) {
    //        case (a, b, c) ⇒ a + b + c
    //      }
    //
    //      // more compact, scalaZ style
    //      val sumUpZ = (operation1 ⊛ operation2 ⊛ operation3) { case (a, b, c) ⇒ a + b + c }
    //
    //      sumUp) shouldBe (1 + 2 + 3)
    //      sumUpZ) shouldBe (1 + 2 + 3)
    //    }
  }
}
