////////////////////////////////////////////////////////////////////////////////
//                                                                            //
//  OpenSolid is a generic library for the representation and manipulation    //
//  of geometric objects such as points, curves, surfaces, and volumes.       //
//                                                                            //
//  Copyright 2007-2015 by Ian Mackenzie                                      //
//  ian.e.mackenzie@gmail.com                                                 //
//                                                                            //
//  This Source Code Form is subject to the terms of the Mozilla Public       //
//  License, v. 2.0. If a copy of the MPL was not distributed with this file, //
//  you can obtain one at http://mozilla.org/MPL/2.0/.                        //
//                                                                            //
////////////////////////////////////////////////////////////////////////////////

package flow

import flow.workflow._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalatest._
import org.scalatest.concurrent._

case class Input(width: Double, height: Double)

class TaskSuite extends FunSuite with Matchers with ScalaFutures {
  def computeTwice(value: Double): Future[Double] = Future.successful(2 * value)

  test("Simple") {
    val getInput = Task.input[Input]

    val getWidth = for { input <- getInput } yield input.width
    val getHeight = getInput.map(_.height)
    val getWidth2 = Task((input: Input) => input.width)
    val getHeight2 = Task.async((input: Input) => Future(input.height))
    val computeArea = for { (width, height) <- parallel(getWidth, getHeight) } yield width * height
    val computePerimeter =
      for {
        (width, height) <- parallel(getWidth2, getHeight2)
      } yield {
        val semiPerimeter = width + height
        2 * semiPerimeter
      }
    val getQuadrupleArea: Task[Input, Double] =
      for {
        (width, height) <- parallel(getWidth, getHeight)
        Seq(twiceHeight, twiceWidth) <- parallel(List(computeTwice(height), computeTwice(width)))
      } yield twiceHeight * twiceWidth

    val failingTask: Task[Input, Int] =
      getWidth.map(width => width.toInt / (width - width).toInt)

    val input = Input(3.0, 4.0)
    getInput.run(input).futureValue.shouldBe(input)
    getWidth.run(input).futureValue.shouldBe(3.0)
    getWidth2.run(input).futureValue.shouldBe(3.0)
    getHeight.run(input).futureValue.shouldBe(4.0)
    getHeight2.run(input).futureValue.shouldBe(4.0)
    computeArea.run(input).futureValue.shouldBe(12.0)
    computePerimeter.run(input).futureValue.shouldBe(14.0)
    getQuadrupleArea.run(input).futureValue.shouldBe(48.0)

    val failedFuture = failingTask.run(input)
    assert(failedFuture.failed.futureValue.isInstanceOf[ArithmeticException])
  }
}
