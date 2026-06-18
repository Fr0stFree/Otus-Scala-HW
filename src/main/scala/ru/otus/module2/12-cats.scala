package ru.otus.module2

import cats.Eval
import cats.data.{Chain, IndexedStateT, Ior, Kleisli, NonEmptyChain, OptionT, State, Validated, ValidatedNec, Writer}
import cats.implicits.catsSyntaxTuple3Semigroupal
import ru.otus.module2.validation.UserDTO

import scala.concurrent.Future
import scala.util.Try


object functional {

  def sum(a: Int, b: Int): (String, Int) = {
    val result = a + b
    (s"Sum result: ${result}", result)
  }

  def double(v: Int): (String, Int) = {
    val result = v * 2
    (s"Double result: ${result}", result)
  }

  // fa: A => B   fb: B => C fa compose fb A => C
  def sumAndDouble(a: Int, b: Int): (String, Int) = {
    val (l1, r1) = sum(a, b)
    val (l2, r2) = double(r1)
    (l1 ++ l2, r2)
  }

  def sum2(a: Int, b: Int): Writer[Chain[String], Int] = {
    val result = a + b
    Writer.value[Chain[String], Int](result).tell(Chain.one(s"Sum result: ${result}"))
  }

  def double2(v: Int): Writer[Chain[String], Int] = {
    val result = v * 2
    Writer.value[Chain[String], Int](result).tell(Chain.one(s"Double result: ${result}"))
  }

  def sumAndDouble2(a: Int, b: Int): Writer[Chain[String], Int] =
    sum2(a, b).flatMap(r => double2(r))







  var counter = 0
  final case class RegNumber private (value: String)

  object RegNumber {
    private def prefix = new StringBuilder("REG-O-")
    def apply(i: Int): RegNumber = new RegNumber(prefix.append(i).toString())
  }

  def regNumber1(): State[Int, RegNumber] = {
    State[Int, RegNumber](i => (i + 1, RegNumber(i + 1)))
  }

  def regNumber2() = {
    counter += 1
    RegNumber(counter)
  }

  def regNumber3() = {
    counter += 1
    RegNumber(counter)
  }


  def regNumbers: State[Int, List[RegNumber]] = for{
    r1 <- regNumber1()
    r2 <- regNumber1()
    r3 <- regNumber1()
  } yield List(r1, r2, r3)




  // Kleisli

  val f1: String => Int = _.toInt
  val f2: Int => Int = i => 10 / i

  // f1 compose f2
  val f3: String => Int = f1 andThen f2

  f3("1") // 10


  val f1Safe: String => Try[Int] = i => Try(i.toInt)
  val f2Safe: Int => Try[Int] = i => Try(10 / i)
  val f3Safe: Kleisli[Try, String, Int] = Kleisli(f1Safe) andThen Kleisli(f2Safe)


  val resultSafe: Try[Int] = f3Safe("")

}

@main
def run1() = println(functional.resultSafe)



object dataStructures{

  // Chain

  val ch1: Chain[Int] = Chain.one(1)
  val ch2: Chain[Int] = Chain.empty[Int]
  val ch3 = Chain(2, 3, 5)
  val ch4 = Chain.fromSeq(List(1, 2, 4))

  // оператор

  val ch5 = ch2 :+ 5
  val ch6 = 5 +: ch2

  // NonEmptyChain
  val nec: NonEmptyChain[Int] = NonEmptyChain.one(1)
  val nec2: NonEmptyChain[Int] = NonEmptyChain(1, 3, 5)
  val nec3: Option[NonEmptyChain[Int]] = NonEmptyChain.fromSeq(List(1, 3))

}

object validation{

  type EmailValidationError = String
  type NameValidationError = String
  type AgeValidationError = String
  type Name = String
  type Email = String
  type Age = Int

  case class UserDTO(email: Email, name: Name, age: Age)
  case class User(email: Email, name: Name, age: Age)

  def emailValidatorE: Either[EmailValidationError, Email] = ???
  def userNameValidatorE: Either[NameValidationError, Name] = ???
  def ageValidatorE: Either[AgeValidationError, Age] = ???


  def validateUserDTO(userDTO: UserDTO): Either[EmailValidationError | NameValidationError | AgeValidationError, User] = for{
    email <- emailValidatorE
    name <- userNameValidatorE
    age <- ageValidatorE
  } yield User(email, name, age)

  // validated

  def emailValidatorV: Validated[EmailValidationError, Email] =
    Validated.valid("email@foo.com")

  def userNameValidatorV: Validated[NameValidationError, Name] =
    Validated.invalid("Invalid name")

  def ageValidatorV: Validated[AgeValidationError, Age] =
    Validated.invalid("Invalid age")

  def validateUserDTOV(userDTO: UserDTO): Validated[NonEmptyChain[String], User] =
    (emailValidatorV.toValidatedNec,
      userNameValidatorV.toValidatedNec,
      ageValidatorV.toValidatedNec).mapN{
      (email, name, age) => User(email, name, age)
    }


  // IoR

  val ior = Ior.Left("Error")
  val ior2 = Ior.Right("Bob")
  val ior3 = Ior.Both("Warning", "Alice")

}
@main
def run2() = println(validation.validateUserDTOV(UserDTO("vffb", "vfvffbv", 12)))



object transformers {
  val f1: Future[String] = Future.successful("2")

  def f2(str: String): Future[Option[Int]] = Future.successful(Try(str.toInt).toOption)

  def f3(i: Int): Future[Option[Int]] = Future.successful(Try(10 / i).toOption)
  
  import scala.concurrent.ExecutionContext.Implicits.global
  
  val r: Future[Int] = for{
    i1 <- f1
    i2 <- f2(i1)
    i2_ <- if(i2.isEmpty) Future.failed(new Exception("f2 failed")) else Future.successful(i2.get)
    i3 <-  f3(i2_)
    i3_ <- if(i3.isEmpty) Future.failed(new Exception("f3 failed")) else Future.successful(i3.get)
  } yield  i2_ + i3_
  
  val r2: OptionT[Future, Int] = for{
    i1 <- OptionT.liftF(f1)
    i2 <- OptionT(f2(i1))
    i3 <- OptionT(f3(i2))
  } yield i2 + i3
  
  val result: Future[Option[Int]] = r2.value
  
  


}