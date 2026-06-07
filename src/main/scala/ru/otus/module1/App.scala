package ru.otus.module1

import ru.otus.module1.concurrency.{MyThread, ToyFuture, future, getRatesLocation1, getRatesLocation2, printRunningTime}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object App {
  def main(args: Array[String]): Unit = {
    println(s"Hello world from: " +
      s"${Thread.currentThread().getName}")

    given ExecutionContext = future.ec

    val f1 = ToyFuture {
      Thread.sleep(1000)
      println("Future 1")
      10
    }(executors.pool1)

    val f2 = ToyFuture {
      Thread.sleep(1000)
      println("Future 2")
      20
    }(executors.pool1)

    val f3 = f1.flatMap{v =>
      f2.map{ v2 =>
        v + v2
      }
    }

    f3.onComplete(println)

//    val r3 = for{
//      v1 <- future.getRatesLocation1
//      v2 <- future.getRatesLocation2
//    } yield v1 + v2
//
//    future.printRunningTime(r3).foreach(println)
  }
}
