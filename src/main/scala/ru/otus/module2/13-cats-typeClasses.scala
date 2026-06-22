package ru.otus.module2

import java.util.Date
import scala.language.postfixOps
import cats.{Contravariant, Functor, Monad, Monoid, Semigroup, Show}
import cats.implicits.given

object catsTypeClasses {

//  trait Show[T]:
//    def show(v: T): String
//
//  object Show {
//    def apply[T](using ev: Show[T]): Show[T] = ev
//    def from[T](f: T => String): Show[T] = v => f(v)
//
//    given showDate: Show[Date] = from[Date](d => s"From epoch start: ${d.getTime}")
//  }
//
//  extension [T](v: T)
//    def show(using ev: Show[T]): String = ev.show(v)

  val d1 = new Date(1000L)
  given Show[Date] = Show.show[Date](d => s"From epoch start: ${d.getTime}")
//  println(d1.show)


//  trait Semigroup[T]:
//    def combine(a: T, b: T): T
//
//  object Semigroup {
//    def apply[T](using ev: Semigroup[T]): Semigroup[T] = ev
//
//    given Semigroup[Int] with
//      override def combine(a: Int, b: Int): Int = a + b
//  }

  // Задача мержа 2-х Map
  val m1 = Map("a" -> 1, "b" -> 2)
  val m2 = Map("b" -> 3, "c" -> 4)
  // val m3 = Map("a" -> 1, "b" -> 5, "c" -> 4)

  def mergeOpt[V: Semigroup](v1: V, optV2: Option[V]): V = optV2 match {
    case Some(v2) => Semigroup[V].combine(v1, v2)
    case None => v1
  }

  def merge[K, V: Semigroup](m1: Map[K, V], m2: Map[K, V]): Map[K, V] =
    m1.foldLeft(m2){ case (acc, (k, v)) =>
      acc.updated(k, mergeOpt(v, acc.get(k)))
    }

//  println(merge(m1, m2))


//  trait Monoid[T] {
//    def combine(a: T, b: T): T
//    def empty: T
//  }

  def combineAll[A: Monoid](l: List[A]): A =
    l.foldLeft(Monoid.empty[A])(Semigroup[A].combine(_, _))

//  println(combineAll(List(1, 2, 3)))

  // Functor

  def doMath[F[_] : Functor](s: F[Int]) = s.map(_ * 2)

//  println(doMath(Option(2)))
//  println(doMath(List(2, 3)))

//  trait Functor2[F[_]]:
//    def map[A, B](fa: F[A])(f: A => B): F[B]
//
//  given func[R]: Functor2[[A] =>> (R) => A] with
//    def map[A, B](fa: R => A)(f: A => B): R => B = fa andThen f


  val f1: Int => String = _.show
  val f2: String => Unit = println
  val f3: Int => Unit = f1 andThen f2
  val f4: Int => Unit = f1 map f2

  f3(10)
  f4(10)

  // Contravariant
  class Id(val raw: String)
  class User(val id: Id)

  val id = Id("reg-1")
  val user = User(id)

  given showId : Show[Id] with
    override def show(t: Id): String = id.raw

  // F[A] B => A
  trait Contravariant2[F[_]] {
    def contramap[A, B](fa: F[A])(f: B => A): F[B]
  }

  given Contravariant2[Show] with
    override def contramap[A, B](fa: Show[A])(f: B => A): Show[B] = ???


  given showUser: Show[User] = Contravariant[Show]
    .contramap[Id, User](showId)(v => v.id)
  println(id.show)
  println(user.show)


  // Invariant
  
  trait Invariant2[F[_]]:
    def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
    
  given Invariant2[Semigroup] = new Invariant2[Semigroup] {
    override def imap[A, B](fa: Semigroup[A])(f: A => B)(g: B => A): Semigroup[B] = new Semigroup[B] {
      override def combine(x: B, y: B): B = {
        val a1: A = g(x)
        val a2: A = g(y)
        val combination: A = fa.combine(a1, a2)
        f(combination)
      }
    }
  }

  given dateSemi: Semigroup[Date] =
    Semigroup[Long].imap(l => new Date(l))(d => d.getTime)

  val now = new Date(1000L)
  val timeLeft = new Date(1000L)
  val result = now |+| timeLeft
  println(result.show)
  
  // Monad
  
//  trait Monad[F[_]] {
//    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
//    def pure[A](v: A): F[A]
//  }
  
  val monad1: Option[Int] = Monad[Option].pure(10)
  val monad2: Option[Int] = Monad[Option].flatMap(monad1)(v => Monad[Option].pure(v + 2))

}

@main
def run3() = catsTypeClasses