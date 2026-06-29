package ru.otus.module3

import ru.otus.module1.concurrency
import zio.*

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}



/** **
 * ZIO[-R, +E, +A] ----> R => Either[E, A]
 *
 */


object toyModel {


  /**
   * Используя executable encoding реализуем свой zio
   */

  case class ZIO[-R, +E, +A](run: R => Either[E, A]) {

    def map[B](f: A => B): ZIO[R, E, B] = flatMap(a => ZIO(r => Right(f(a))))

    def flatMap[R1 <: R, E1 >: E, B](f: A => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
      ZIO(r => this.run(r).fold(
        e => ZIO.fail(e),
        v => f(v)
      ).run(r))

  }



  /**
   * Реализуем конструкторы под названием effect и fail
   */

  object ZIO {

    def effect[A](a: => A): ZIO[Any, Throwable, A] = try{
      ZIO(_ => Right(a))
    } catch {
      case e: Throwable => fail(e)
    }

    def fail[E](e: E): ZIO[Any, E, Nothing] = ZIO(_ => Left(e))
  }



  /** *
   * Напишите консольное echo приложение с помощью нашего игрушечного ZIO
   */

  val z1 = ZIO.effect(StdIn.readLine())
  def z2(str: String) = ZIO.effect(println(str))

  // z1.flatMap(str => z2(str))

//  type Task[+A] = ZIO[Any, Throwable, A]

  lazy val echo: ZIO[Any, Throwable, Unit] = for{
    str <- z1
    _ <- z2(str)
  } yield ()

//  val greet: Task[Unit] = for {
//    _ <- ZIO.effect(println("Как тебя зовут?"))
//    name <- ZIO.effect(StdIn.readLine())
//    _ <- ZIO.effect(println(s"Привет, $name"))
//  } yield ()







  type Error
  type Environment



  lazy val _: Task[Int] = ??? // ZIO[Any, Throwable, Int]
  lazy val _: IO[Error, Int] = ??? // ZIO[Any, Error, Int]
  lazy val _: RIO[Environment, Int] = ??? // ZIO[Environment, Throwable, Int]
  lazy val _: URIO[Environment, Int] = ??? // ZIO[Environment, Nothing, Int]
  lazy val _: UIO[Int] = ??? // ZIO[Any, Nothing, Int]
}

object zioConstructors {


  // констант
  val z1: UIO[Int] = ZIO.succeed(7)

  // любых эффектов
  val z2: Task[Unit] = ZIO.attempt(println("Hello"))


  val z3: Task[String] = ZIO.async{ (cb: Task[String] => Unit) =>
    concurrency.getRatesLocation1.onComplete {
      case Failure(exception) => cb(ZIO.fail(exception))
      case Success(value) => cb(ZIO.succeed(s"Result : $value"))
    }
  }

  val future: Future[String] = Future.successful("Hello world")


  // Из Future
  lazy val z4: Task[String] = ZIO.fromFuture(_ => future)



  // Из try
  lazy val t: Try[String] = ???
  val z5: Task[String] = ZIO.fromTry(t)



  // Из option
  lazy val opt : Option[Int] = ???
  val z6: IO[Option[Nothing], Int] = ZIO.fromOption(opt)
  val z7: UIO[Option[Int]] = z6.option
  val z8: IO[Option[Nothing], Int] = z7.some


  type User
  type Address


  def getUser(): Task[Option[User]] = ???
  def getAddress(u: User): Task[Option[Address]] = ???

  val r = for{
    user <- getUser().some
    address <- getAddress(user)
  } yield address //



  // Из either
  lazy val e: Either[String, Int] = Right(10)

  val z9: IO[String, Int] = ZIO.fromEither(e)
  val z10: UIO[Either[String, Int]] = z9.either
  val z11: IO[String, RuntimeFlags] = z10.absolve


  // From function

  val z12: ZIO[String, Throwable, Unit] =
    ZIO.fromFunction[String => Unit](str => println(str))



  // особые версии конструкторов

  val _ = ZIO.unit
  val _ = ZIO.none
  val _ = ZIO.never // while (true)
  val _ = ZIO.die(new Throwable("Ooops"))



}



object zioOperators {

  /** *
   *
   * 1. Создать ZIO эффект, который будет читать строку из консоли
   */

  lazy val readLine = ???

  /** *
   *
   * 2. Создать ZIO эффект, который будет писать строку в консоль
   */

  def writeLine(str: String) = ???

  /** *
   * 3. Создать ZIO эффект котрый будет трансформировать эффект содержащий строку в эффект содержащий Int
   */

  lazy val lineToInt = ???
  /** *
   * 3. Создать ZIO эффект, который будет работать как echo для консоли
   *
   */

  lazy val echo = ???

  /**
   * Создать ZIO эффект, который будет приветствовать пользователя и говорить, что он работает как echo
   */

  lazy val greetAndEcho = ???



  // greet and echo улучшенный
  lazy val _: ZIO[Any, Throwable, Unit] = ???


  /**
   * Используя уже созданные эффекты, написать программу, которая будет считывать поочереди считывать две
   * строки из консоли, преобразовывать их в числа, а затем складывать их
   */

  lazy val r1 = ???

  /**
   * Второй вариант
   */

  lazy val r2: ZIO[Any, Throwable, Int] = ???

  /**
   * Доработать написанную программу, чтобы она еще печатала результат вычисления в консоль
   */

  lazy val r3 = ???


  lazy val a: Task[Int] = ???
  lazy val b: Task[String] = ???
  lazy val c: Task[Unit] = ???

  /**
   * Последовательная комбинация эффектов a и b
   */
  lazy val ab1: Task[(Int, String)] = a zip b

  /**
   * Последовательная комбинация эффектов a и b
   */
  lazy val ab2: ZIO[Any, Throwable, String] = a zipRight  b

  /**
   * Последовательная комбинация эффектов a и b
   */
  lazy val ab3: Task[Int] = a zipLeft b


  /**
   * Последовательная комбинация эффекта b и b, при этом результатом должна быть конкатенация
   * возвращаемых значений
   */
  lazy val ab4 = a.zipWith(a)(_ + _)




  /**
    * 
    * A as B
    */
  

}

@main
def run4() = {
  val z = ZIO.attempt(println("Hello world"))
  Unsafe.unsafe{ implicit unsafe =>
    zio.Runtime.default.unsafe.run(z)
  }
}

object MyApp extends ZIOAppDefault {
  val z = ZIO.attempt(println("Hello world"))
  def run = z
}