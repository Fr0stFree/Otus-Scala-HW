package ru.otus.module3

import zio.{Exit, IO, Task, UIO, URIO, Unsafe, ZIO}

import java.io.IOException
import scala.io.StdIn


object zioErrorHandling {

  sealed trait Cause[+E]

  object Cause {

    final case class Fail[E](e: E) extends Cause[E]

    final case class Die(t: Throwable) extends Cause[Nothing]

  }


  case class ZIO[-R, +E, +A](run: R => Either[E, A]) {

    /**
      * 
      * Базовый оператор для работы с ошибками
      */

    def foldM[R1 <: R, E1, B](
             failure: E => ZIO[R1, E1, B],
             success: A => ZIO[R1, E1, B]
             ): ZIO[R1, E1, B] =
      ZIO(r => this.run(r).fold(
        e => failure(e),
        a => success(a)
      ).run(r))

    def orElse[R1 <: R, E1, A1 >: A](other: ZIO[R1, E1, A1]): ZIO[R1, E1, A1] =
      foldM(
        _ => other,
        v => ZIO(r => Right(v))
      )

    def option: ZIO[R, Nothing, Option[A]] =
      foldM(
        _ => ZIO(r => Right(None)),
        v => ZIO(r => Right(Some(v)))
      )

    def flatMap[R1 <: R, E1 >: E, B](f: A => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
      foldM(
        e => ZIO(_ => Left(e)),
        v => f(v)
      )

    def mapError[E1](f: E => E1): ZIO[R, E1, A] =
      foldM(
        e => ZIO(_ => Left(f(e))),
        v => ZIO(_ => Right(v))
      )





    /**
     * Реализовать метод, который будет игнорировать ошибку в случае падения,
     * а в качестве результата возвращать Option
     */




    /**
     * Реализовать метод, который будет работать с каналом ошибки
     */




  }











  sealed trait IntegrationError

  case object ConnectionFailed extends IntegrationError

  case object CommunicationFailed extends IntegrationError

  lazy val connect: IO[ConnectionFailed.type, Unit] = ???

  lazy val getSomeData: IO[CommunicationFailed.type, String] = ???


  lazy val connectAndGetData: zio.ZIO[Any, IntegrationError, String] =
    connect zipRight getSomeData


  lazy val io1: IO[String, String] = ???

  lazy val io2: IO[Int, String] = ???

  /**
   * 1. Какой будет тип на выходе, если мы скомбинируем эти два эффекта с помощью zip
   */

   lazy val z1: zio.ZIO[Any, Any, (String, String)] = io1 zip io2

  /**
   * Можем ли мы как-то избежать потерю информации об ошибке, в случае композиции?
    */

  lazy val io3: zio.ZIO[Any, Either[String, Int], (String, String)] =
    io1.mapError(e => Left(e)).zip(io2.mapError(e => Right(e)))


  def either: Either[String, Int] = ???

  def errorToErrorCode(str: String): Int = ???

  lazy val effFromEither: IO[String, Int] = zio.ZIO.fromEither(either)

  /**
   * Изменить ошибку effFromEither
   */

  lazy val z3: zio.ZIO[Any, Int, Int] = effFromEither.mapError(errorToErrorCode)

  /**
   * Логировать ошибку effFromEither, не меняя ее тип и тип возвращаемого значения
   */
  lazy val z2 = effFromEither.tapError(e => zio.ZIO.attempt(println(e)))





  // Разные типы ошибок

    type User = String
    type UserId = Int

    sealed trait NotificationError
    case object NotificationByEmailFailed extends NotificationError
    case object NotificationBySMSFailed extends NotificationError
//    case object UserNotFound extends NotificationError



    def getUserById(userId: UserId): Task[User] = ???

    def sendEmail(user: User, msg: String): IO[NotificationByEmailFailed.type, Unit] = ???

    def sendSMS(user: User, msg: String): IO[NotificationBySMSFailed.type, Unit] = ???

    def sendNotification(userId: UserId): zio.ZIO[Any, NotificationError, Unit] = for{
      user <- getUserById(userId).orDie
      _ <- sendEmail(user, "email")
      _ <- sendSMS(user,"9999" )
    } yield ()

   def readFile(name: String): Task[List[String]] = ???

   lazy val z4 = ???
}

@main
def run7() = {
  val z = ZIO.succeed(10 / 0)
  val result = Unsafe.unsafe { implicit unsafe =>
    zio.Runtime.default.unsafe.run(z.catchAllDefect(_ => ZIO.unit))
  }
  println(result)
}

