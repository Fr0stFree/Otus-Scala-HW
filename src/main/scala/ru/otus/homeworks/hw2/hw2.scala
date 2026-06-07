package ru.otus.homeworks.hw2

import scala.util.Random
import scala.collection.mutable

object hw2 {
  val random = new Random()

  sealed trait Ball
  object Ball {
    case object White extends Ball
    case object Black extends Ball
  }

  case class Bucket(balls: mutable.ArrayBuffer[Ball]) {
    def retrieve(): Ball = balls.remove(random.nextInt(balls.size))
  }

  object Bucket {
    def apply(white: Int, black: Int): Bucket = {
      Bucket(
        mutable.ArrayBuffer.fill(white)(Ball.White) ++ mutable.ArrayBuffer.fill(
          black
        )(Ball.Black)
      )
    }
  }

  object Experiment {
    def hasWhiteBall(): Boolean = {
      val bucket = Bucket(white = 3, black = 3)
      val firstBall = bucket.retrieve()
      val secondBall = bucket.retrieve()
      firstBall == Ball.White || secondBall == Ball.White
    }

    def run(): Unit = {
      val amountOfExperiments = 100000
      val successCount = (1 to amountOfExperiments)
        .map(_ => hasWhiteBall())
        .filter(identity)
        .size

      println(s"Success count: $successCount")
      println(s"Success rate: ${successCount.toDouble / amountOfExperiments}")
    }
  }

}
