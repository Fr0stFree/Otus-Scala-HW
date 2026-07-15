package ru.otus.module3

import zio.{Duration, IO, RIO, Tag, Task, UIO, Unsafe, ZEnvironment, ZIO, durationInt}

import scala.language.postfixOps

object di {

  type Query[_]
  type DBError
  type QueryResult[_]
  type Email = String

  trait User{
    def email: String
  }


  trait DBService{
    def tx[T](query: Query[T]): IO[DBError, QueryResult[T]]
  }

  trait EmailService{
    def makeEmail(email: String, body: String): Task[Email]
    def sendEmail(email: Email): Task[Unit]
  }

  trait LoggingService{
    def log(str: String): Task[Unit]
  }

  trait UserService{
    def getUserBy(id: Int): RIO[LoggingService, User]
    def id: Int
  }


  /**
   * Написать эффект, который напечатает в консоль приветствие, подождет 5 секунд,
   * сгенерит рандомно число, напечатает его в консоль
   *   Console
   *   Clock
   *   Random
   */

  val z1: ZIO[Random & Clock & Console, Nothing, Unit] = for{
    console <- ZIO.service[Console]
    clock <- ZIO.service[Clock]
    random <- ZIO.service[Random]
    _ <- console.printLine("Hello")
    _ <- clock.sleep(5 seconds)
    int <- random.nextInt()
    _ <- console.printLine(int.toString)
  } yield ()


  trait Console{
    def printLine(v: Any): UIO[Unit]
  }

  trait Clock{
    def sleep(duration: Duration): UIO[Unit]
  }

  trait Random{
    def nextInt(): UIO[Int]
  }


  lazy val z2: ZIO[String, Throwable, Unit] = ???
  lazy val z3: ZIO[Int, Throwable, Unit] = ???


  /**
   * Эффект, который будет комбинацией двух эффектов выше
   */

  lazy val z4: ZIO[Int & String, Throwable, Unit] = z2 zipRight z3

  // f1: String => Unit
  // f2: Int => Unit
  // f3: (String, Int) => Unit = (str, int) => f1(str); f2(int)


  /**
   * Написать ZIO программу, которая выполнит запрос и отправит email
   */


  lazy val services: ZEnvironment[UserService with EmailService with LoggingService] = ???

  lazy val dBService: DBService = ???
  lazy val userService: UserService = ???

  lazy val emailService2: EmailService = ???

  def f(userService: ZEnvironment[UserService]): ZEnvironment[UserService with EmailService with LoggingService] = ???


  lazy  val queryAndNotify: ZIO[LoggingService with EmailService with UserService, Throwable, Unit] = ???

  lazy val z5: IO[Throwable, Unit] = queryAndNotify.provideEnvironment(services)

  lazy val z6: ZIO[UserService, Throwable, Unit] = queryAndNotify.provideSomeEnvironment[UserService](f)

  trait ToyScope {
    def close: UIO[Unit]
    def addFinalizer[A](f: => UIO[Any]):UIO[Unit]
  }

  object ToyScope {
    def withFinalizer[R, E, A](zio: ZIO[R, E, A])(finalizer: A => UIO[Any]): ZIO[R with ToyScope, E, A] = {
      zio.flatMap{a =>
        ZIO.serviceWithZIO[ToyScope](_.addFinalizer(finalizer(a)) *>
          ZIO.succeed(a))
      }
    }

    private def f[R: Tag](e: ZEnvironment[R]): ZEnvironment[R with ToyScope] = {
      val ts = new ToyScope {
        val finalizers = scala.collection.mutable.ListBuffer.empty[UIO[Any]]

        override def close: UIO[Unit] = ZIO.collectAll(finalizers.toList).unit

        override def addFinalizer[A](f: => UIO[Any]): UIO[Unit] =
          ZIO.succeed(finalizers.addOne(f))
      }
      val ze: ZEnvironment[ToyScope with R] = ZEnvironment(ts).++[R](e)
      ze
    }

    def toyScoped[R: Tag, E, A](zio: ZIO[R with ToyScope, E, A]): ZIO[R, E, A] = {
      zio.flatMap{a =>
        ZIO.serviceWithZIO[ToyScope](_.close) *> ZIO.succeed(a)
      }.provideSomeEnvironment[R](zr => f[R](zr))
    }
  }

  val z7: ZIO[ToyScope, Throwable, Unit] = ToyScope.withFinalizer(ZIO.attempt(println("Hello 1")))(_ =>
    ZIO.succeed(println("Finalizer 1")))

  val z8: ZIO[ToyScope, Throwable, Unit] = ToyScope.withFinalizer(ZIO.attempt(println("Hello 2")))(_ =>
    ZIO.succeed(println("Finalizer 2")))

  val z9: ZIO[ToyScope, Throwable, Unit] = z7 zipRight z8

  val z10: ZIO[Any, Throwable, Unit] = ToyScope.toyScoped(z9)

}


@main
def run10() = Unsafe.unsafe { implicit unsafe =>
  zio.Runtime.default.unsafe.run(di.z10)
}