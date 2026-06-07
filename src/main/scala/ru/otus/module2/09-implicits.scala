package ru.otus.module2

import ru.otus.module2.conv.{shout, given }
object conv {
  class StringOps(str: String) {
    def trimToOption: Option[String] = Option(str).map(_.trim).filter(_.nonEmpty)
  }

  extension (s: String)
    def shout: String = s.toUpperCase

  given Conversion[String, StringOps] with
    override def apply(x: String): StringOps = new StringOps(x)
}

object implicits {

  // implicit conversions

  object implicit_conversions {


    // преобразовывать String => Int

    // val c: Conversion[String, Int] = str => Integer.parseInt(str)

    given Conversion[String, Int] with
      override def apply(x: String): Int = Integer.parseInt(x)

    val result = "84" / 42


    /** Расширить возможности типа String, методом trimToOption, который возвращает Option[String]
      * если строка пустая или null, то None
      * если нет, то Some от строки со всеми удаленными начальными и конечными пробелами
      */



//    given Conversion[String, StringOps] with
//      override def apply(x: String): StringOps = new StringOps(x)



    val str = "  Hello"

    val result2 = str.trimToOption
    val result3 = str.shout



    case class Currency(amount: Double)
    case class USD(amount: Double)
    case class ExchangeRate(rateToUSD: Double)

    given ExchangeRate(1.3)
    // implicit val rate: ExchangeRate = ExchangeRate(1.3)

    def currencyToUSD(amount: Currency)(using ex: ExchangeRate): USD =
      USD(amount.amount * ex.rateToUSD)

    def pay(amount: USD) = ???

    val currency: Currency = ???

    pay(currencyToUSD(currency))

    // implicit def currencyToUsd(x: Currency)(implicit ex: ExchangeRate): USD = ???
    given (using rate: ExchangeRate): Conversion[Currency, USD] with
      override def apply(x: Currency): USD = USD(x.amount * rate.rateToUSD)

    pay(currency)

  }


  object implicit_scopes {

    trait Printable

    trait Printer[T] extends Printable {
      def print(v: T): Unit
    }

    object Printable {
//       given v: Printer[Bar] = new Printer[Bar] {
//         override def print(v: Bar): Unit = println(s"Implicit from companion object Printable + $v")
//       }
    }

    // companion object Printer
    object Printer {
//       given Printer[Bar] = new Printer[Bar] {
//         override def print(v: Bar): Unit = println(s"Implicit from companion object Printer + $v")
//       }
    }

    case class Bar()

    case class Foo()


    // companion object Bar
    object Bar {
        given v: Printer[Bar] = new Printer[Bar] {
          override def print(v: Bar): Unit = println(s"Implicit from companion object Bar + $v")
        }
    }

    // some arbitrary object
    object wildcardImplicits {
      given v: Printer[Bar] = new Printer[Bar] {
        override def print(v: Bar): Unit = println(s"Implicit from wildcard import + $v")
      }
    }

    def print[T](b: T)(using m: Printer[T]) = m.print(b)

     given Printer[Bar] = new Printer[Bar]{
       def print(v: Bar): Unit = println(s"Implicit from local val + $v")
     }

//    import wildcardImplicits.{given }

    print(Bar())

  }

}

@main
def run = implicits.implicit_scopes