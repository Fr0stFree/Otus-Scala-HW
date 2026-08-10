package ru.otus.module3.cats.http4s

import cats.effect.{IO, IOApp}
import org.http4s.{Http, HttpApp, HttpRoutes}
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{Host, Port}
import org.http4s.server.Router

//1
/*object Restfull {
  val service: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello" /name => Ok(name)
  }

  val httpApp: Http[IO, IO] = service.orNotFound

  val server1 = for {
    s <- EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(8080).get)
      .withHttpApp(httpApp).build
  } yield s
}

object  mainServer extends  IOApp.Simple {
  def run(): IO[Unit] = {
    Restfull.server1.use( _ => IO.never)
  }
}*/

//2
object RestFull2Endpoints {

  val serviceOne: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello1" /name => IO.raiseError(new RuntimeException("ERROR!!!!!")) *> Ok(s"web service one")
    case POST -> Root / "hello2" /name => Ok(s"web service from $name")
  }

  val serviceTwo: HttpRoutes[IO] = {
    HttpRoutes.of{
      case GET -> Root/"hello2"/name => Ok("web service Ok2")
    }
  }

  val service: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello" /name => Ok("web service 0")
  }

  val router = Router (
    "/" -> serviceOne,
    "/api" -> serviceTwo,
    "/apiroot" -> service
  )

  import cats.syntax.all._
  import org.http4s.dsl.io._
  import org.http4s._
  val httpApp: HttpApp[IO] = HttpApp[IO] { req =>
    router.orNotFound(req).handleErrorWith {
      e =>
        IO.println(s"error ${e.getMessage}") *> InternalServerError("Global server error")
    }
  }


  val server = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString("localhost").get)
    .withPort(Port.fromInt(8080).get)
    .withHttpApp(httpApp).build
}

object  mainServer2 extends  IOApp.Simple {
  def run: IO[Unit] = {



    RestFull2Endpoints.server.use( _ => IO.never)
  }
}
