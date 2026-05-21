package ru.otus.module1



import ru.otus.module1.variance.{Animal, Cat}

import scala.language.postfixOps



/**
 * referential transparency
 */


 // recursion

object recursion {

  /**
   * Реализовать метод вычисления n!
   * n! = 1 * 2 * ... n
   */

  def fact(n: Int): Int = {
    var _n = 1
    var i = 2
    while (i <= n){
      _n *= i
      i += 1
    }
    _n
  }


  def factRec(n: Int): Int =
    if(n <= 0) 1 else n * factRec(n - 1)


  def factTailRec(n: Int): Int = {
    def loop(n: Int, accum: Int): Int =
      if(n <= 0) accum
      else loop(n - 1, n * accum)
    loop(n, 1)
  }



  /**
   * Реализовать вычисление N числа Фибоначчи
   * F0 = 0, F1 = 1, Fn = Fn-1 + Fn - 2
   */


}



object hof{

  def dumb(string: String): Unit = {
    Thread.sleep(1000)
    println(string)
  }

  // обертки

  def logRunningTime[A, B](f: A => B): A => B = a =>
    val start = System.currentTimeMillis()
    val result = f(a)
    val end = System.currentTimeMillis()
    println(end - start)
    result



  // изменение поведения ф-ции

  def isOdd(i: Int): Boolean = i % 2 > 0
  lazy val isEven: Int => Boolean = not(isOdd)
  def not[A](f: A => Boolean): A => Boolean = a => !f(a)



  // изменение самой функции

  def sum(x: Int, y: Int): Int = x + y

  def curried[A, B, C](f: (A, B) => C): A => B => C = a => b => f(a, b)

  curried(sum) // Int => Int => Int

  def partial2[A, B, C](a: A, f: (A, B) => C): B => C = curried(f)(a)

  val r: Int => Int = partial2(2, sum)
  r(3) // 5

}


object variance {


  // Invariance Вне зависимости от отношений между типами A и B, Box[A] и Box[B] два разных типа
  // + Covariance Если А является подтипом В, то Box[A] является подтипом Box[B]
  // - Contravariance Если А является подтипом В, то Box[A] является супер типом Box[B]

  class Box[+T](val item: T)

  class Feeder[-T] {
    def feed(v: T): Unit = println("Feeding")
  }

  sealed trait Animal

  case class Cat() extends Animal

  case class Dog() extends Animal

  val animalFeeder: Feeder[Animal] = Feeder[Animal]()
  val catFeeder: Feeder[Cat] = catFeeder
  catFeeder.feed(Cat())

  def feed(a: Animal): Unit = ???

  feed(Cat())
  feed(Dog())

  // trait Function1[-R, +T] = R => T

  val f1 : Animal => Dog = ???
  val f2: Dog => Animal = f1

}
