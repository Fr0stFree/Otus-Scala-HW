package ru.otus.module1



object adt {

  object tuples {

    /**
     * Tuples ()
     */

    object Foo


    type ProductFooBoolean = (Foo.type, Unit)

    /** Создать возможные экземпляры с типом ProductFooBoolean
      */

    val v1: ProductFooBoolean = (Foo, ())




    /** Реализовать тип Person который будет содержать имя и возраст
      */

    type Person = (String, Int)

    val v2: Person = ("John", 35)



    /**  Реализовать тип `CreditCard` который может содержать номер (String),
      *  дату окончания (java.time.YearMonth), имя (String), код безопасности (Short)
      */

    type CreditCard = (String, java.time.YearMonth, String, Short)

  }






  object case_classes {

    /**
     * Case classes
     */



    //  Реализовать Person с помощью case класса

    case class Person(name: String, age: Int)

    // Создать экземпляр для Tony Stark 42 года
    val tony = Person("Tony", 42)


    // Создать case class для кредитной карты

  }







  object either {

    /** Sum
      */

    /** Either - это наиболее общий способ хранить один из двух или более кусочков информации в одно время.
      * Так же как и кортежи обладает целым рядом полезных методов
      * Иммутабелен
      */


    object Bar


    type BarOrBoolean = Either[Bar.type, Boolean]

    val v1: BarOrBoolean = Left(Bar)
    val v2: BarOrBoolean = Right(true)
    val v3: BarOrBoolean = Right(false)






    /** Реализовать экземпляр типа IntOrString с помощью конструктора Right
      */


    type IntOrString


    object CreditCard
    object WireTransfer
    object Cash

    /**
      * Реализовать тип PaymentMethod который может быть представлен одной из альтернатив
      */
    type PaymentMethod = Either[CreditCard.type, Either[WireTransfer.type , Cash.type]]

    val p1: PaymentMethod = Left(CreditCard)
    val p2: PaymentMethod = Right(Right(Cash))
    val p3: PaymentMethod = Right(Left(WireTransfer))

  }

  object sealed_traits {

    /** Также Sum type можно представить в виде sealed trait с набором альтернатив
      */

    sealed trait PaymentMethod
    case object CreditCard extends PaymentMethod
    case object WireTransfer extends PaymentMethod
    case object Cash extends PaymentMethod

    val p1: PaymentMethod = CreditCard
    val p2: PaymentMethod = WireTransfer
    val p3: PaymentMethod = Cash

  }


  object enums {

    enum Color(val rgb: Int):
      val p: Int = 10
      case Red extends Color(0xFF0000)
      case Green extends Color(0x00FF00)
      case Blue extends Color(0x0000FF)

    val c1: Color = Color.Red
    val c2: Color = Color.Green
    val c3: Color = Color.Blue

    val _c1 = c1.ordinal
    val colors: Array[Color] = Color.values
    val red = Color.valueOf("Red")
    val green = Color.fromOrdinal(1)


  }




  object cards {

    type Suit                           // масть
    type Clubs                          // крести
    type Diamonds                       // бубны
    type Spades                         // пики
    type Hearts                         // червы
    type Rank                           // номинал
    type Two                            // двойка
    type Three                          // тройка
    type Four                           // четверка
    type Five                           // пятерка
    type Six                            // шестерка
    type Seven                          // семерка
    type Eight                          // восьмерка
    type Nine                           // девятка
    type Ten                            // десятка
    type Jack                           // валет
    type Queen                          // дама
    type King                           // король
    type Ace                            // туз
    type Card                           // карта
    type Deck                           // колода
    type Hand                           // рука
    type Player                         // игрок
    type Game                           // игра
    type PickupCard                     // взять карту

  }

}
