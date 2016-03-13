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

import scala.concurrent.{Future, ExecutionContext}

trait Task[T, R] { self =>
  def run(input: T)(implicit context: ExecutionContext): Future[R]

  def map[U](function: (R) => U): Task[T, U] =
    new Task[T, U] {
      override def run(input: T)(implicit context: ExecutionContext): Future[U] =
        self.run(input).map(function)
    }

  def flatMap[U](function: R => Future[U]): Task[T, U] =
    new Task[T, U] {
      override def run(input: T)(implicit context: ExecutionContext): Future[U] =
        self.run(input).flatMap(function)
    }

  def withFilter(predicate: (R) => Boolean): Task[T, R] =
    new Task[T, R] {
      override def run(input: T)(implicit context: ExecutionContext): Future[R] =
        self.run(input).withFilter(predicate)
    }

  def andThen[U](next: Task[R, U]): Task[T, U] =
    new Task[T, U] {
      override def run(input: T)(implicit context: ExecutionContext): Future[U] = {
        for {
          intermediate <- self.run(input)
          result <- next.run(intermediate)
        } yield result
      }
    }

  def compose[U](previous: Task[U, T]): Task[U, R] =
    new Task[U, R] {
      override def run(input: U)(implicit context: ExecutionContext): Future[R] = {
        for {
          intermediate <- previous.run(input)
          result <- self.run(intermediate)
        } yield result
      }
    }
}

object Task {
  def apply[T, R](function: (T) => R): Task[T, R] =
    new Task[T, R] {
      override def run(input: T)(implicit context: ExecutionContext): Future[R] =
        Future(function(input))
    }

  def async[T, R](function: (T) => Future[R]): Task[T, R] =
    new Task[T, R] {
      override def run(input: T)(implicit context: ExecutionContext): Future[R] =
        function(input)
    }

  def input[T]: Task[T, T] =
    new Task[T, T] {
      override def run(input: T)(implicit context: ExecutionContext): Future[T] =
        Future.successful(input)
    }
}

object workflow {
  def parallel[T1, T2](firstFuture: Future[T1], secondFuture: Future[T2])
                      (implicit context: ExecutionContext): Future[(T1, T2)] =
    for {
      firstValue <- firstFuture
      secondValue <- secondFuture
    } yield (firstValue, secondValue)

  def parallel[T](futures: Seq[Future[T]])(implicit context: ExecutionContext): Future[Seq[T]] =
    Future.sequence(futures)

  def parallel[T, R1, R2](firstTask: Task[T, R1], secondTask: Task[T, R2]): Task[T, (R1, R2)] =
    new Task[T, (R1, R2)] {
      override def run(input: T)(implicit context: ExecutionContext): Future[(R1, R2)] =
        parallel(firstTask.run(input), secondTask.run(input))
    }

  def parallel[T, R](tasks: Seq[Task[T, R]]): Task[T, Seq[R]] =
    new Task[T, Seq[R]] {
      override def run(input: T)(implicit context: ExecutionContext): Future[Seq[R]] =
        parallel(tasks.map(_.run(input)))
    }
}
