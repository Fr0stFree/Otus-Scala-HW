package ru.otus.module3.catsmiddleware

import cats.data.{Kleisli, Reader}

import scala.concurrent.{Await, Future}


// type Reader[R,A] = Kleisli[Id, R, A]
// Reader R => A
// Kleisli R=>F[A]
// Kleisli[F, A, B]

object KleisliReader {

  def main(args: Array[String]): Unit ={
    case class Config(dbUrl: String, dbUser: String, dbPassword: String)

    val dbUrlReader: Reader[Config, String] = Reader(config => config.dbUrl)
    val dbPasswordReader: Reader[Config, String] = Reader(config => config.dbPassword)
    val dbUserReader: Reader[Config, String] = Reader(config => config.dbUser)

    val fullInfo: Reader[Config, String] = for {
      url <- dbUrlReader
      user <- dbUserReader
      password <- dbPasswordReader
    } yield s"Db url: $url, user: $user, password: $password"

    val config = Config("test","test","test")
    val result = fullInfo.run(config)
    println(result)
  }
}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object catsMiddlewareKleisli {
  def getUserById(id: Int): Future[Option[String]] = Future.successful(Some("test"))
  def getOrderByUserName(name: String): Future[Option[String]] = Future.successful(Some("test1"))

  val getUserK: Kleisli[Future, Int, Option[String]] = Kleisli(getUserById)
  val getOrderK: Kleisli[Future, String, Option[String]] = Kleisli(getOrderByUserName)

  def main(args: Array[String]): Unit = {
    val getUserOrderK: Kleisli[Future, Int, Option[String]] = getUserK.flatMap{
      case Some(name) => Kleisli.liftF(getOrderK.run(name)) //Kleisli[Future, Int, Option[String]]
      case None => Kleisli.liftF[Future, Int, Option[String]](Future.successful(None))
    }

    val resultFuture: Future[Option[String]] = getUserOrderK.run(1)

    resultFuture.foreach{
      case Some(order) => println(s"order: $order")
      case None => println("No order found")
    }

    Await.result(resultFuture, 10.seconds)

  }

}

// f: A => B
// g: B => C
// h = f andThne g // A=>C

// f: A => Future[B]
//g: B => Future[C]
// f andThen g
// a => f(a).flatMap(b=>g(b))
//Kleisli
// A => F[B]