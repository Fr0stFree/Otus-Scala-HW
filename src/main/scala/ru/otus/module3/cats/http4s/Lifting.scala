package ru.otus.module3.cats.http4s
import cats.data.OptionT
import cats.effect.{IO, IOApp}

object Lifting extends IOApp.Simple {

  case class User(id: Int, name: String)

  // Уже находится в нашем "большом" контексте
  // case class OptionT[F[_], A](value: F[Option[A]])
  def findUser(id: Int): OptionT[IO, User] =
    OptionT {
      IO {
        if (id == 1)
          Some(User(1, "Alex"))
        else
          None
      }
    }

  // Только IO — Option здесь нет
  def loadAge(user: User): IO[Int] =
    IO {
      println(s"Loading age for ${user.name}...")
      25
    }

  // Вообще обычное значение
  val country: String =
    "Germany"

  val program: OptionT[IO, String] =
    for {
      user <- findUser(1) // OptionT[IO, User]

      // IO[Int] поднимаем в OptionT[IO, Int]
      age <- OptionT.liftF(loadAge(user)) // IO[Int] -> OptionT[IO, Int] ;   OptionT(loadAge(user).map(age => Some(age)))
      //OptionT[IO, Int]

      // обычное значение поднимаем в OptionT
      userCountry <- OptionT.pure[IO](country)
      // OptionT[IO, String]

    } yield {
      s"${user.name}, age = $age, country = $userCountry"
    }



  override def run: IO[Unit] =
    program.value.flatMap(result => IO.println(result))
}