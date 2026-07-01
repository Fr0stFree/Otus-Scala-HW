package ru.otus.module3

import zio.*

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import scala.language.postfixOps
import scala.concurrent.Future
import scala.util.{Failure, Success}


object zioConcurrency {


  // эффект содержит в себе текущее время
  val currentTime: UIO[Long] = Clock.currentTime(TimeUnit.SECONDS)


  /**
   * Напишите эффект, который будет считать время выполнения любого эффекта
   */

  // 1. Получить время
  // 2. Включить эффект в цепочку
  // 3. Получить время
  // 4. Вывести разницу

  def printEffectRunningTime[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] = 
    for {
      start <- currentTime
      r <- zio
      end <- currentTime
      _ <- ZIO.attempt(println(s"Running time: ${end - start}")).orDie
    } yield r
  

  val exchangeRates: Map[String, Double] = Map(
    "usd" -> 76.02,
    "eur" -> 91.27
  )

  /**
   * Эффект который все что делает, это спит заданное кол-во времени, в данном случае 1 секунду
   */
  lazy val sleep1Second: UIO[Unit] = ZIO.sleep(1 seconds)

  /**
   * Эффект который все что делает, это спит заданное кол-во времени, в данном случае 3 секунды
   */
  lazy val sleep3Seconds = ZIO.sleep(3 seconds)

  /**
   * Создать эффект, который печатает в консоль GetExchangeRatesLocation1 спустя 3 секунды
   */
  lazy val getExchangeRatesLocation1 = sleep3Seconds *> 
    ZIO.attempt(println("GetExchangeRatesLocation1"))

  /**
   * Создать эффект, который печатает в консоль GetExchangeRatesLocation2 спустя 1 секунду
   */
  lazy val getExchangeRatesLocation2 = sleep1Second *> 
    ZIO.attempt(println("GetExchangeRatesLocation2"))


  /**
   * Написать эффект, который получит курсы из обеих локаций
   */

  lazy val getFrom2Locations = getExchangeRatesLocation1 *> getExchangeRatesLocation2


  /**
   * Написать эффект, который получит курсы из обеих локаций параллельно
   */
  
  lazy val getFrom2LocationsPar = for{
    f1 <-getExchangeRatesLocation1.fork
    f2 <-getExchangeRatesLocation2.fork
    _ <- f1.join
    _ <- f2.join
  } yield ()
  


  /**
   * Предположим нам не нужны результаты, мы сохраняем в базу и отправляем почту
   */


  lazy val writeUserToDB = sleep3Seconds zipRight 
    ZIO.attempt(println("User saved"))

  lazy val sendMail = sleep1Second zipRight 
    ZIO.attempt(println("Mail sent"))

  /**
   * Написать эффект, который сохранит в базу и отправит почту параллельно
   */

  lazy val writeAndSend = for{
    _ <- writeUserToDB.fork
    _ <- sendMail.fork
  } yield ()


  /**
   *  Greeter
   */

  lazy val greeter: ZIO[Any, Throwable, Nothing] = 
    (sleep1Second zipRight ZIO.attempt(println("Hello"))) *> greeter

  val g1   = for{
    f1 <- greeter.fork
    _ <- sleep3Seconds
    _ <- f1.interrupt
  } yield ()
  


  /***
   * Greeter 2
   *
   *
   *
   */
  
  def imperativeGreeter(canceled: AtomicBoolean) = {
    while (true && canceled.get()) {
      Thread.sleep(1000)
      println("Hello")
    }
  }


  lazy val greeter2 = ZIO.attempt(imperativeGreeter)
  
  val g2 = for{
    ref <- ZIO.succeed(new AtomicBoolean(true))
    _ <- ZIO.attemptBlockingCancelable(imperativeGreeter(ref))(ZIO.succeed(ref.set(false))).fork
    _ <- sleep3Seconds
  } yield ()


  /**
   * Прерывание эффекта
   */

  lazy val app3 = ???





  /**
   * Получение информации от сервиса занимает 1 секунду
   */
  def getFromService(ref: Ref[Int]) = ???

  /**
   * Отправка в БД занимает в общем 5 секунд
   */
  def sendToDB(ref: Ref[Int]): ZIO[Clock, Exception, Unit] = ???


  /**
   * Написать программу, которая конкурентно вызывает выше описанные сервисы
   * и при этом обеспечивает сквозную нумерацию вызовов
   */


  lazy val app1 = ???

  /**
   *  Concurrent operators
   */
  
  val z = getExchangeRatesLocation1 zip getExchangeRatesLocation2 // 4
  val z2 = getExchangeRatesLocation1 zipPar getExchangeRatesLocation2 // 3

  val z3: ZIO[Any, Throwable, Either[Int, String]] = ZIO.attempt(10) raceEither  ZIO.attempt("Hello")
  
  val z4 = ZIO.foreachPar(List(1, 2, 3, 4, 5)){ i =>
    sleep1Second *> ZIO.attempt(println(s"Hello: ${i}"))
  }


  /**
   * Lock
   */


  // Правило 1
  lazy val doSomething: UIO[Unit] = ???
  lazy val doSomethingElse: UIO[Unit] = ???

  lazy val executor: Executor = ???

  lazy val eff = for{
    f1 <- doSomething.fork
    _ <- doSomethingElse
    r <- f1.join
  } yield r

  lazy val result = eff.onExecutor(executor)



  // Правило 2
  lazy val executor1: Executor = ???
  lazy val executor2: Executor = ???



  lazy val eff2 = for{
    f1 <- doSomething.onExecutor(executor2).fork
    _ <- doSomethingElse
    r <- f1.join
  } yield r

  lazy val result2 = eff2.onExecutor(executor)



}


object ConcurrencyApp extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = 
    zioConcurrency.printEffectRunningTime(zioConcurrency.z4)
}