package ru.otus.module2


/**
 * 1. Синтаксические изменения
 */

@main
def foo = println("Hello")

  object SyntaxScala2{
     if(true) {
       println()
       println()
     } else {
       println()
       println()
     }

     class Foo
     val foo = new Foo

     for (i <- 1 to 5){
       println(i)
     }
  }

  object SyntaxScala3{
    if true then
      println()
      println()
    else
      println()
      println()
    end if



    class Foo
    val foo = Foo()

    for i <- 1 to 5 do
      println(i)
      println(i)
      println(i)
      println(i)
    end for

  }


/**
 * 2. Enums
 */


  enum Color:
    case Red
    case Green
    case Blue

  sealed trait Color2

  case object Red extends Color2
  case object Green extends Color2
  case object Blue extends Color2


/**
 * 3. New types
 */


trait A:
  def methodA(): Unit

trait B:
  def methodB(): Unit

////trait AwithB extends A with B
//type AwithB = A with B

def handle(v: A & B): Unit =
  v.methodA()
  v.methodB()

def handle2(v: Either[A, B]): Unit = v match {
  case Left(a) => println(a.methodA())
  case Right(b) => println(b.methodB())
}


object Logarithm:
  opaque type Log = Double
  def create(v: Double): Log = math.log(v)
  def value(log: Log): Double = math.exp(log)
  def plus(a: Log, b: Log): Log = value(a) + value(b)


val r = Logarithm.plus(Logarithm.create(10), Logarithm.create(20))
// val r2 = Logarithm.plus(10, 20)
/**
 * 4. Зависимые методы и функции
 */

trait Entry:
  type Key
  def key: Key

def extractKey(e: Entry): e.Key = e.key

val extractor: (e: Entry) => e.Key = extractKey

/**
 * 5. Полиморфные функции
 */

 val reverse: [A] => List[A] => List[A] = [A] => (list: List[A]) => list.reverse


/**
 * 6. Контекстные абстракции
 */

 given Int = 10
 // implicit val i: Int = 10

 def handle(using Int) = ???

 def handle2(implicit i: Int) = ???


 given Conversion[String, Int] with
   override def apply(x: String): Int = Integer.parseInt(x)

 implicit def strToInt(str: String): Int = Integer.parseInt(str)

 extension (v: String)
   def trimToOption: Option[String] = ???

// implicit class StrOps(s: String) {
//   def trimToOption: Option[String] = ???
// }

 val r3 = "vcfdvbf".trimToOption

 trait Ordering[T]

 def sort[T: Ordering](list: List[T]): List[T] = {
   val ord = implicitly[Ordering[T]]
   ???
 }

 type Condition[T] = T ?=> Boolean


object PostConditions:
  opaque type Wrap[T] = T

  def result[T](using t: Wrap[T]): T = t

  extension [T](v: T)
    def ensure(cond: Wrap[T] ?=> Boolean): T =
      assert(cond(using v))
      v
import PostConditions.{result, ensure}

val r4 = List(1, 2, 3).sum.ensure(result[Int] == 6)