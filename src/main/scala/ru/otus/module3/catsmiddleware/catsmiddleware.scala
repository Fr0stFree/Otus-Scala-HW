package ru.otus.module3.catsmiddleware


import cats.Functor
import cats.data.{Kleisli, OptionT}
import cats.effect.kernel.Ref
import cats.effect.{IO, IOApp, Resource}
import org.http4s.{AuthedRequest, AuthedRoutes, Http, HttpApp, HttpRoutes, Status}
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{Host, Port}
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.ci.CIStringSyntax
import org.http4s.{Method, Request, Status, Uri}
import cats.implicits.toSemigroupKOps

object Restfull {
  val service: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello" / name => Ok("1")
  }

  val serviceOne: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello1" / name => Ok("2")
  }

  val serviceTwo: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "hello2" / name => Ok("3")
  }

  val router = Router(
    "/" -> serviceOne,
    "/api" -> serviceTwo,
    "/apiroot" -> service
  )

  val httpApp = router.orNotFound

  //1
  val server = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString("localhost").get)
    .withPort(Port.fromInt(8081).get)
    .withHttpApp(httpApp).build

  //2
  def addresponseMiddleware[F[_] : Functor](routes: HttpRoutes[F]): HttpRoutes[F] = Kleisli {
    req =>
      val maybeResponse = routes(req)
      maybeResponse.map {
        case Status.Successful(resp) => resp.putHeaders("X-Otus" -> "test")
        case other => other
      }
  }

  val router2 = addresponseMiddleware(Router(
    "/" -> addresponseMiddleware(serviceOne),
    "/api" -> addresponseMiddleware(serviceTwo),
    "/apiroot" -> addresponseMiddleware(service)
  ))

  val httpApp2 = router2.orNotFound

  val server2 = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString("localhost").get)
    .withPort(Port.fromInt(8081).get)
    .withHttpApp(httpApp2).build

  //3 session

  type Session[F[_]] = Ref[F, Set[String]]

  def serviceSession(sessions: Session[IO]): HttpRoutes[IO] = {
    HttpRoutes.of {
      case r@GET -> Root / "hello" =>
        r.headers.get(ci"X-User-Name") match {
          case Some(values) =>
            val name = values.head.value
            sessions.get.flatMap(users =>
              if (users.contains(name)) Ok(s"hello, $name")
              else Forbidden("no access")
            )
        }
      case PUT -> Root / "login" / name =>
        sessions.update(set => set + name).flatMap(_ => Ok("done"))
    }
  }


  def routerSessions(sessions: Session[IO]): HttpRoutes[IO] =
    addresponseMiddleware(Router("/" -> serviceSession(sessions)))

  val serverSession = for {
    session <- Resource.eval(Ref.of[IO, Set[String]](Set.empty))
    s <- EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(8081).get)
      .withHttpApp(routerSessions(session).orNotFound).build

  } yield s

  // 4 auth

  def loginService(session: Session[IO]): HttpRoutes[IO] =
    HttpRoutes.of {
      case PUT -> Root/"login"/name =>
        session.update(set => set + name).flatMap(_ => Ok("done"))
    }

  def serviceHelloAuth: AuthedRoutes[User, IO] = AuthedRoutes.of {
    case GET -> Root / "hello" as user =>
      Ok(s"Hello, ${user.name}")
  }

  final case class User(name: String)

  def routerSessionAuth(sessions: Session[IO]): HttpRoutes[IO] = {
    addresponseMiddleware(Router("/" -> (loginService(sessions) <+> serviceAuthMiddleware(sessions)(serviceHelloAuth))))
  }

  def serviceAuthMiddleware(sessions: Session[IO]): AuthMiddleware[IO, User] =
    authRoutes =>
      Kleisli { req =>
        req.headers.get(ci"X-User-Name") match {
          case Some(values) =>
            val name = values.head.value
            for {
              users <- OptionT.liftF(sessions.get)
              results <-
                if (users.contains(name)) authRoutes(AuthedRequest(User(name), req))
                else
                  OptionT.liftF(Forbidden("no access"))
            } yield results
          case None => OptionT.liftF(Forbidden("no access"))
        }
      }


  val serverSessionAuthServer = for {
    session <- Resource.eval(Ref.of[IO, Set[String]](Set.empty))
    s <- EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(8081).get)
      .withHttpApp(routerSessionAuth(session).orNotFound).build

  } yield s


}

/*
object mainServer extends IOApp.Simple {
  def run: IO[Unit] = {
//    Restfull.server2.use(_=>IO.never)
//    Restfull.serverSession.use(_=>IO.never)
    Restfull.serverSessionAuthServer.use(_ => IO.never)
  }
}*/