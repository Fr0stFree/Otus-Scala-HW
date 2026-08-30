package ru.otus.homeworks.Homework10

import cats.effect.{IO, Ref}
import fs2.{Chunk, Stream}
import io.circe.Json
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.io._

import scala.concurrent.duration._

case class SlowParams(chunkSize: Int, totalSize: Int, delay: FiniteDuration)

object AppRoutes {
  private val xByte = 'x'.toByte

  private def toSlowStream(params: SlowParams): Stream[IO, Byte] = {
    val chunks = Stream
      .iterate(0)(_ + params.chunkSize)
      .takeWhile((remaining) => remaining < params.totalSize)

    chunks.zipWithIndex.flatMap { case (offset, index) =>
      val size = math.min(params.chunkSize, params.totalSize - offset)
      val chunk = Stream.chunk(Chunk.array(Array.fill(size)(xByte)))
      if (index == 0) then chunk
      else Stream.sleep_[IO](params.delay) ++ chunk
    }
  }

  def router(counter: Ref[IO, Int]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "counter" =>
        counter
          .updateAndGet(_ + 1)
          .flatMap(value => Ok(Json.obj("counter" -> Json.fromInt(value))))

      case GET -> Root / "slow" / chunk / total / time =>
        val params = for {
          chunkSize <- chunk.toIntOption.toRight(s"Invalid chunk size: $chunk")
          totalSize <- total.toIntOption.toRight(s"Invalid total size: $total")
          delay <- time.toIntOption.toRight(s"Invalid delay: $time")
        } yield SlowParams(chunkSize, totalSize, delay.seconds)

        params match {
          case Left(error)   => BadRequest(error)
          case Right(params) => Ok(toSlowStream(params))
        }
    }
  }
}
