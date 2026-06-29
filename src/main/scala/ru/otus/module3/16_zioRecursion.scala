package ru.otus.module3

import zio.{Task, Unsafe, ZIO}

import scala.io.StdIn

object zioRecursion {



  /** 
   * Создать ZIO эффект, который будет читать число в виде строки из консоли
   */

  lazy val readLine: Task[String] = ZIO.attempt(StdIn.readLine())



  /** *
   * Воспользовавшись readLine создайте ZIO, который прочитает строку из консоли
   * и вернет число (Int)
   */

  lazy val readInt: Task[Int] = readLine.flatMap(str => ZIO.attempt(str.toInt))


  /**
   * Написать программу, которая считывает из консоли Int введенный пользователем,
   * а в случае ошибки, сообщает о некорректном вводе, и просит ввести заново
   *
   */

   lazy val readIntOrRetry: Task[Int] = readInt.orElse{
     ZIO.attempt(println("Некорректный ввод, попробуй еще раз")) zipRight readIntOrRetry
   }



  /**
   * Считаем факториал
   */



  def factorial(n: BigInt): BigInt = {
    if(n <= 1) n
    else n * factorial(n - 1)
  }

  def fib(n: Long): Long = {
      if(n == 0 || n == 1) n
      else fib(n - 1) + fib(n - 2)
  }

  /**
   * Написать ZIO версию ф-ции факториала
   *
   */

  def factorialZ(n: BigInt): Task[BigInt] =
    if (n <= 1) ZIO.succeed(n)
    else ZIO.succeed(n).zipWith(factorialZ(n - 1))(_ * _)


}

@main
def run6() = {

  val N = 15000
//  println(zioRecursion.factorial(N))
  Unsafe.unsafe { implicit unsafe =>
    zio.Runtime.default.unsafe.run(zioRecursion.factorialZ(N)
      .flatMap(i => ZIO.attempt(println(i))))
  }
}