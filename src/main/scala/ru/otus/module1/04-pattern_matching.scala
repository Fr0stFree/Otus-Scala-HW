package ru.otus.module1

object pattern_matching{

  /**
   * Сопоставление с типом
   */

  val i: Any = List(10)

  i match {
    case v: Int => println("Int")
    case v: Double => println("Double")
    case v: String => println("String")
    case v: List[String] => println("List[String]")
    case v: List[Int] => println("List[Int]")
    case _ => println("Something else")
  }




  /**
   * Структурный сопоставление
   */



  sealed trait Animal{

    def whoIam: Unit = this match {
      case Dog(n, a) => println(s"I'm dog $n")
      case Cat(n, a) => println(s"I'm cat $n")
    }
  }


  case class Dog(name: String, age: Int) extends Animal
  class Cat(val name: String, val age: Int) extends Animal

  object Cat {
    def unapply(c: Cat): Option[(String, Int)] = Some((c.name, c.age))
  }


  val url = "/users/john/profile"

  object Fragments {
    def unapplySeq(path: String): Option[Seq[String]] = Some(path.split('/').filter(_.nonEmpty))
  }

  url match {
    case Fragments("users", userName, "profile") => println(userName)
    case _ => println("Smth else")
  }


  val str = "His name is John"

  str match {
    case s"His name is ${name}" => println(s"Hello, ${name}")
  }


  /**
   * Сопоставление с литералом
   */

  lazy val animal: Animal = Dog("Bim", 10)

  animal match {
    case Dog("Grey", age) => println("I'm Grey")
    case Dog(n, a) => println(s"I'm dog $n")
    case Cat(name, age) => println(s"I'm cat $name")
  }


  val Bim = "Bim"


  animal match {
    case Dog(Bim, age) => println("I'm Bim")
    case Dog(n, a) => println(s"I'm dog $n")
    case Cat(name, age) => println(s"I'm cat $name")
  }

  /**
   * Сопоставление с константой
   */



  /**
   * Сопоставление с условием (гарды)
   */

  animal match {
    case Dog(n, age) if n == "Grey" => println(s"I'm ${n}")
    case Dog(n, a) => println(s"I'm dog $n")
    case Cat(name, age) => println(s"I'm cat $name")
  }



  /**
   * Биндинги ("as" паттерн)
   */

  def treatCat(cat: Cat) = ???
  def treatDog(dog: Dog) = ???


  def treat(a: Animal) = a match {
    case d @ Dog(name, age) => treatDog(d)
    case c @ Cat(name, age) => treatCat(c)
  }



  /**
   * Используя сопоставление напечатать имя и возраст
   */



  
  case class Employee(name: String, address: Address)
  case class Address(val street: String, val number: Int)
  
  
  case class Person(name: String, age: Int)
  
  val p: Person = Person("Ivan", 22)
  
  val Person(name, age) = p


/**
 * Воспользовавшись сопоставлением напечатать номер из поля адрес
 */





/**
 * Сопоставление может содержать литералы.
 * Реализовать Сопоставление на Alex с двумя кейсами.
 * 1. Имя должно соответствовать Alex
 * 2. Все остальные
 */




/**
 * Паттерны могут содержать условия. В этом случае case сработает,
 * если и паттерн совпал и условие true.
 * Условия в Сопоставление называются гардами.
 */



/**
 * Реализовать Сопоставление на Alex с двумя кейсами.
 * 1. Имя должно начинаться с A
 * 2. Все остальные
 */


/**
 *
 * Мы можем поместить кусок паттерна в переменную использую `as` паттерн,
 * x @ ..., где x это любая переменная.
 * Это переменная может использоваться, как в условии,
 * так и внутри кейса
 */

  trait PaymentMethod
  case object Card extends PaymentMethod
  case object WireTransfer extends PaymentMethod
  case object Cash extends PaymentMethod

  case class Order(paymentMethod: PaymentMethod)

  lazy val order: Order = ???

  lazy val pm: PaymentMethod = ???


  def checkByCard(o: Order) = ???

  def checkOther(o: Order) = ???



/**
 * Мы можем использовать вертикальную черту `|` для сопоставления на альтернативы
 */

 sealed trait A
 case class B(v: Int) extends A
 case class C(v: Int) extends A
 case class D(v: Int) extends A

 lazy val a: A = B(10)

 a match {
   case B(_) | C(_) =>
   case D(_) =>
 }



}