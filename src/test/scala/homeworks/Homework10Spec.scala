package homeworks

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import org.http4s.{Status, Uri, Request, Method}
import org.http4s.client.Client
import org.http4s.implicits._
import org.typelevel.ci.CIString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.Json

import ru.otus.homeworks.Homework10.AppRoutes

class Homework10Spec extends AnyFlatSpec with Matchers {
  "GET /counter" should "increment counter" in {
    val actual = (
      for {
        counter <- Ref.of[IO, Int](0)
        client = Client.fromHttpApp(AppRoutes.router(counter).orNotFound)
        body <- client.expect[String](Request[IO](Method.GET, uri"/counter"))
      } yield body
    ).unsafeRunSync()

    val expected = Json.obj("counter" -> Json.fromInt(1)).noSpaces

    actual shouldBe expected
  }

  "GET /counter" should "increment counter for each request" in {
    val actual = (
      for {
        counter <- Ref.of[IO, Int](0)
        client = Client.fromHttpApp(AppRoutes.router(counter).orNotFound)
        first <- client.expect[String](Request[IO](Method.GET, uri"/counter"))
        second <- client.expect[String](Request[IO](Method.GET, uri"/counter"))
        third <- client.expect[String](Request[IO](Method.GET, uri"/counter"))
      } yield (first, second, third)
    ).unsafeRunSync()

    actual shouldBe (
      Json.obj("counter" -> Json.fromInt(1)).noSpaces,
      Json.obj("counter" -> Json.fromInt(2)).noSpaces,
      Json.obj("counter" -> Json.fromInt(3)).noSpaces
    )
  }

  "GET /slow" should "return requested amount of data" in {
    val actual = (
      for {
        counter <- Ref.of[IO, Int](0)
        client = Client.fromHttpApp(AppRoutes.router(counter).orNotFound)
        body <- client.expect[String](
          Request[IO](Method.GET, uri"/slow/10/25/0")
        )
      } yield body
    ).unsafeRunSync()

    actual shouldBe "x" * 25
  }

  it should "return Bad Request for invalid parameters" in {
    val actual = (for {
      counter <- Ref.of[IO, Int](0)
      client = Client.fromHttpApp(AppRoutes.router(counter).orNotFound)

      invalidChunk <- client.status(
        Request[IO](Method.GET, uri"/slow/foo/100/0")
      )
      invalidTotal <- client.status(
        Request[IO](Method.GET, uri"/slow/10/foo/0")
      )
      invalidTime <- client.status(
        Request[IO](Method.GET, uri"/slow/10/100/foo")
      )
    } yield (invalidChunk, invalidTotal, invalidTime))
      .unsafeRunSync()

    actual shouldBe (Status.BadRequest, Status.BadRequest, Status.BadRequest)
  }
}
