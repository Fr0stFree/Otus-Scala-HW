package module3

import ru.otus.module3.emailService.EmailAddress
import ru.otus.module3.userService.{User, UserID}
import ru.otus.module3.zioDS
import zio.test.Assertion.*
import zio.test.TestAspect.nonFlaky
import zio.test.*
import zio.{Clock, Console, Random, ZIO, ZLayer, durationInt}

import java.io.IOException
import scala.language.postfixOps


object BasicZIOSpec extends ZIOSpecDefault{

  val env = ZLayer.make[Random with Console with Clock](
    ZLayer.succeed(Random.RandomLive),
    ZLayer.succeed(zio.Console.ConsoleLive),
    ZLayer.succeed(zio.Clock.ClockLive))

  val greeter: ZIO[Any, IOException, Unit] = for{
    _ <- zio.Console.printLine("Как тебя зовут")
    name <- zio.Console.readLine
    _ <- zio.Console.printLine(s"Привет, $name")
    age <- zio.Console.readLine
    _ <- zio.Console.printLine(s"Age $age")
  } yield ()



  // generation

  val intGen: Gen[Any, Int] = Gen.int
  val idGen: Gen[Any, UserID] = intGen.map(UserID)


  override def spec = suite("Basic")(
    suite("Arithmetic")(
      test("2*2")(
        assert(2 * 2)(equalTo(4))
      ),
      test("division by zero")(
        assert(2 / 0)(throws(isSubtype[ArithmeticException](anything)))
      )
    ),
    suite("effect testing")(
      test("simple effect")(
        assertZIO(ZIO.succeed(2*2))(equalTo(4))
      ),
      test("test console")(
        for{
          _ <- TestConsole.feedLines("Alex", "18")
          _ <- greeter
          value <- TestConsole.output
        } yield assert(value)(hasFirst(equalTo("Как тебя зовут\n")))
      ),
      test("concurrent execution")(
        for{
          fiber <- (zio.Console.printLine("Hello") *> ZIO.sleep(5 seconds)).fork
          _ <- TestClock.adjust(5 seconds)
          _ <- fiber.join
          output <- TestConsole.output
        } yield assert(output(0))(equalTo("Hello\n"))
      ),
      test("test failing zio")(
        assertZIO(ZIO.attempt(2 / 0).exit)(failsWithA[ArithmeticException])
      ),
      test("test counter")(
        assertZIO(zioDS.ref.updateCounterRef)(equalTo(3))
      ) @@ nonFlaky
    ),
    suite("property based testing")(
      test("int addition is associative")(
        check(intGen, intGen, intGen){ case (x, y, z) =>
          val left = (x + y) + z
          val right = x + (y + z)
          assert(left)(equalTo(right))
        }
      )
    )
  )

}
