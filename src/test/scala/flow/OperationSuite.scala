package flow

import flow.OperationImplicits._

import org.scalatest.{ShouldMatchers, WordSpec}
import util.Logging

import scala.util.Success
//import scalaz.syntax.bind._

class OperationSuite extends WordSpec with ShouldMatchers with Logging {
  trait DummyExecutor extends ExecutorU {
    override def execute[A](operation: Operation[A]): A = operation.apply()
  }

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

      "Transformer examples" should {

    val transformerIntToString = Transformer[Int, String] { i: Int => i.toString }
    val transformerAppendBang = Transformer[String, String] { s: String => s + "!" }
    val transformerAppendHash = Transformer[String, String] { s: String => s + "#" }
    val transformerConcatenate = Transformer[(String, String), String] { s: (String, String) => s._1 + s._2 }

    "linear" in new DummyExecutor {

      val result = for {
        s <- transformerIntToString(3)
        ss <- transformerAppendBang(s)
      } yield ss

      execute(result) shouldBe "3!"
    }

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
    }
  }

  "Simple examples" should {
    "operation as function" in new DummyExecutor {
      val operationInt = Operation { 123 }
      val operationString = Operation { "mama" }

      val operationWithDependencies =
        (first: Int, second: String) ⇒ Operation { first.toString + second }

      val merged = for {
        i ← operationInt
        s ← operationString
        together ← operationWithDependencies(i, s)
      } yield together

      execute(merged) shouldBe "123mama"
    }

    "operation as class" in new DummyExecutor {
      val operationInt = Operation { 123 }
      val operationString = Operation { "mama" }

      case class SomeMergedGuy(first: Int, second: String) {
        def apply() = Operation { first.toString + second }
      }

      val merged = for {
        i ← operationInt
        s ← operationString
        together ← SomeMergedGuy(i, s)()
      } yield together

      execute(merged) shouldBe "123mama"
    }

    "operation as wrapper" in new DummyExecutor {
      class WrappedOperation {
        def op1 = Operation { 123 }
        def apply() = op1.map(_ * 2)
      }

      val operationInt = (new WrappedOperation)()
      execute(operationInt) shouldBe 246
    }

    "operation as function instance" in new DummyExecutor {
      class FunctionOperation extends (() ⇒ Operation[Int]) {
        def apply() = Operation { 123 }
      }

      val operationInt = new FunctionOperation
      execute(operationInt()) shouldBe 123
    }

    "longer computation" in new DummyExecutor {
      case class Provider1(first: Int, second: Int) {
        def apply() = Operation { first + second }
      }

      case class Provider2(first: String, second: String) {
        def apply() = Operation { first.length + second.length }
      }

      val myProvider = for {
        r1 ← Provider1(1, 1)()
        r2 ← Provider2("a", "bcd")()
        summed ← Provider1(r1, r2)()
        // these could go inside yield
        a = 123
        b = 23
      } yield a - b + summed

      execute(myProvider) shouldBe (100 + 2 + 4)
    }

    "simple fill" in new DummyExecutor {
      case class Provider(first: Int, second: Int) {
        def apply() = Operation { first + second }
      }

      val quiteFilled = (second: Int) ⇒ for {
        p ← Provider(500, second)()
      } yield p

      execute(quiteFilled(250)) shouldBe 750
    }

    "bit more complex fill" in new DummyExecutor {
      case class Provider(first: Int, second: Int) {
        def apply() = Operation { first + second }
      }

      val harder = (first: Int, second: Int) ⇒ for {
        p1 ← Provider(500, second)()
        p2 ← Provider(first, 100)()
      } yield p1 + p2

      execute(harder(-500, -100)) shouldBe 0

      val another = for {
        x ← harder(-500, -100)
        y ← Provider(3, 7)()
      } yield x + y

      execute(another) shouldBe 10
    }

    "generic operation should compile" in new DummyExecutor {
      case class Generic[A]() {
        def apply() = Operation { 555 }
      }

      val z = new Generic[Int]()
      z().map(_.toString)
      z().flatMap(null)

      for { x ← Generic[Int]()() } yield ()
    }
  }

  "Advanced examples" should {
    "Partial graph fill" in new DummyExecutor {
      val IProduceInt = Operation { 666 }

      case class ITake3Arguments(first: Int, second: Boolean, third: String) {
        def apply() = Operation { first.toString + second.toString + third.toString }
      }

      // also could be another case class
      val firstHoleFilled = (second: Boolean) ⇒ for {
        i ← IProduceInt
        m ← ITake3Arguments(i, second, "mama")()
      } yield m

      execute(firstHoleFilled(false)) shouldBe "666falsemama"
    }

    "Mutating operation (prehaps for that dynamic case on whiteboard?)" in new DummyExecutor {
      val IProduceInt = Operation { 10 }

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
        r ← (new DynamicOperation(i, xs))()
      } yield r

      execute(dynamicOperation) shouldBe 10 + 0
      xs += "test"
      execute(dynamicOperation) shouldBe 10 + 4
    }

  }

  "Tutorials" should {
    // if you miss/don't know something either come to me (marek kadek)
    // or feel free to expand.

    // Monad allows us to chain computation.
    // - you can use value from operation1 in operation2, if it was needed, i.e. ...b <- operation2(a)
    // - first problem stops the computation. i.e. if operation2 died (exception), operation3 would not apply
    // - hence is not parallelizable (because next steps may depend on previous)
    "Monad" in new DummyExecutor {
      val operation1 = Operation { 1 }
      val operation2 = Operation { 2 }
      val operation3 = Operation { 3 }

      // we don't use next stuff we from previous computations... it's so that I can show same example with
      // applicative
      val sumUp = for {
        a ← operation1
        b ← operation2
        c ← operation3
      } yield a + b + c

      execute(sumUp) shouldBe (1 + 2 + 3)
    }

    // Applicatives are less common but just as useful as monads.
    // - the operations are independent
    // - all computations apply, i.e. if operation2 dies (exception), operation 1 and 3 would apply
    // - hence is parallelizable - there is no dependency on context (no `previous step`)
//    "Applicative" in new DummyExecutor {
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
//      execute(sumUp) shouldBe (1 + 2 + 3)
//      execute(sumUpZ) shouldBe (1 + 2 + 3)
//    }
  }
}
