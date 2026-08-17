package ru.otus.module4.phoneBook.api

import io.circe.parser.decode
import io.circe.syntax._
import ru.otus.module4.phoneBook.db.DataSource
import ru.otus.module4.phoneBook.dto.PhoneRecordDTO
import ru.otus.module4.phoneBook.services.PhoneBookService
import ru.otus.module4.phoneBook.services.PhoneBookService.PhoneRecordNotFound
import zio._
import zio.http._
import zio.http.codec.PathCodec.string

object PhoneBookAPI {

  private def jsonResponse(json: String, status: Status = Status.Ok): Response =
    Response(
      status = status,
      headers = Headers(Header.ContentType(MediaType.application.json)),
      body = Body.fromString(json)
    )

  private def badRequest(message: String): Response =
    jsonResponse(Map("error" -> message).asJson.noSpaces, Status.BadRequest)

  val api: Routes[PhoneBookService.Service & DataSource, Response] =
    Routes(
      Method.GET / string("phone") -> handler { (phone: String, _: Request) =>
        PhoneBookService
          .find(phone)
          .map(result => jsonResponse(result.asJson.noSpaces))
          .catchAll {
            case _: PhoneRecordNotFound => ZIO.succeed(Response.status(Status.NotFound))
            case error                  => ZIO.succeed(badRequest(error.getMessage))
          }
      },

      Method.POST / Root -> handler { (req: Request) =>
        (for {
          body   <- req.body.asString
          dto    <- ZIO.fromEither(decode[PhoneRecordDTO](body))
          result <- PhoneBookService.insert(dto)
        } yield jsonResponse(Map("id" -> result).asJson.noSpaces, Status.Created))
          .catchAll(error => ZIO.succeed(badRequest(error.getMessage)))
      },

      Method.PUT / string("id") / string("addressId") -> handler {
        (id: String, addressId: String, req: Request) =>
          (for {
            body <- req.body.asString
            dto  <- ZIO.fromEither(decode[PhoneRecordDTO](body))
            _    <- PhoneBookService.update(id, addressId, dto)
          } yield Response.ok)
            .catchAll(error => ZIO.succeed(badRequest(error.getMessage)))
      },

      Method.DELETE / string("id") -> handler { (id: String, _: Request) =>
        PhoneBookService
          .delete(id)
          .as(Response.ok)
          .catchAll(error => ZIO.succeed(badRequest(error.getMessage)))
      }
    )
}
