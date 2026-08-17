package ru.otus.module3.catsmiddleware


import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import io.circe.{Decoder, Json, ParsingFailure}
import io.circe.parser.parse
import org.http4s.{HttpRoutes, Method, Request, Uri}
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import io.circe.generic.auto._


object CircyJson extends IOApp.Simple{
  case class User(name: String, email: Option[String])
  //1
  /*  implicit val decoderUser: Decoder[User] = Decoder.instance(
      cur =>
        for {
          name <- cur.downField("name").as[String]
          email <- cur.downField("email").as[Option[String]]
        } yield User(name, email)
    )*/

  val example = """{"name": "111", "email": "111@222.de"}"""
  val json: Either[ParsingFailure, Json] = parse(example)

  //2 semiauto
  implicit val decoderUser: Decoder[User] = Decoder.derived


  def run: IO[Unit] = IO.println{
    for {
      json <- parse(example)
      user <- json.as[User]
    } yield user
  }
}

import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

object restfulldesc {
  def publicRoutes: HttpRoutes[IO] = HttpRoutes.of {
    case r@POST -> Root / "echo" =>
      for {
        u <- r.as[CircyJson.User]
        _ <- IO.println(u)
        response <- Ok(u)
      } yield response
  }
  val router = Router("/public" -> publicRoutes)
  val server = for {
    s<- EmberServerBuilder
      .default[IO]
      .withPort(Port.fromInt(8080).get)
      .withHost(Host.fromString("localhost").get)
      .withHttpApp(router.orNotFound).build
  } yield s
}

object HttpClientCircy {
  val builder = EmberClientBuilder.default[IO].build
  val postrequest = Request[IO](
    method = Method.POST,
    uri = Uri.fromString("http://localhost:8080/public/echo").toOption.get)
    .withEntity(CircyJson.User("sdfsdg", Some("zsdfsdf@sdfsdf.de")))

  val result = builder.use (
    client => client.run(postrequest).use(
      resp =>
        if (resp.status.isSuccess)
          resp.as[CircyJson.User]
        else
          IO.raiseError(new Exception("error"))
    )
  )
}

object MainCircyPars extends IOApp.Simple {
  def run: IO[Unit] = for {
    _<- restfulldesc.server.use(_ =>
      HttpClientCircy.result.flatMap(IO.println) *> IO.never)
  } yield ()
}

import fs2.Stream

object Http4sStreamExampleFromClient extends IOApp {
  def service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "upload" =>
      val stream: Stream[IO, Byte] = req.body
      ???
  }

  override def run(args: List[String]): IO[ExitCode] = {
    //todo run web server
    ???
  }
}


object Http4sStreamExampleToClient extends IOApp {
  def service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ Method.GET -> Root / "download" =>
      val stream: Stream[IO, Byte] = Stream.emits("mxsdfnmkjdfnk".getBytes)

      Ok(stream)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    //todo run web server
    ???
  }
}

