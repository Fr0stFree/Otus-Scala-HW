package ru.otus.module3.cats.http4s

import cats.data.OptionT

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

object Transformers:

  case class User(id: Int, name: String)
  case class Address(city: String)

  def findUser(id: Int): Future[Option[User]] =
    Future.successful {
      if id == 1 then Some(User(1, "Alex"))
      else None
    }

  def findAddress(user: User): Future[Option[Address]] =
    Future.successful {
      if user.id == 1 then Some(Address("Berlin"))
      else None
    }

  def main(args: Array[String]): Unit =

    val result: OptionT[Future, Address] =
      for
        user    <- OptionT(findUser(1)) // Future[Option[User]] -> OptionT[Future, User]
        address <- OptionT(findAddress(user))
      yield address

    val rawResult: Future[Option[Address]] =
      result.value

    println(Await.result(rawResult, 3.seconds))
