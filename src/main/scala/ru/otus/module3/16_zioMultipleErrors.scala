package ru.otus.module3

import zio.{IO, Task, Unsafe, ZIO}
import zio.Cause.Both


sealed trait Error extends Product
case object E1 extends Error
case object E2 extends Error

object multipleErrors{
    val z1: IO[E1.type, Int] = ZIO.fail(E1)

    val z2: IO[E2.type, Int] = ZIO.fail(E2)

    lazy val result: ZIO[Any, Error, Either[Int, Int]] = z1 raceEither  z2

    lazy val app = result.tapErrorCause{
        case Both(left, right) =>
            ZIO.attempt(println(left.failureOption)) zipRight
              ZIO.attempt(println(right.failureOption))
    }

}

@main
def run8() = {

    val z: ZIO[Any, Throwable, Unit] = ZIO.attempt(println("Hi"))
      .flatMap(_ => ZIO.attempt(println("By")))

    Unsafe.unsafe { implicit unsafe =>
        zio.Runtime.default.unsafe.run(z)
    }
}
