package ru.otus.module3.cats

import cats.{Defer, MonadError}
import cats.effect.*
import cats.effect.kernel.*
import cats.syntax.all.*

import scala.concurrent.duration.*

object CatsEffectHierarchyDemo extends IOApp.Simple:

  // MonadError: ошибка как часть эффекта
  def monadErrorExample[F[_]](
                               using F: MonadError[F, Throwable]
                             ): F[String] =
    F.raiseError[String](
      new RuntimeException("тестовая ошибка")
    ).handleError { error =>
      s"Ошибка обработана: ${error.getMessage}"
    }

  // Defer: отложенное построение эффекта
  def deferExample[F[_]](
                          using F: Defer[F]
                        ): F[String] =
    F.defer {
      // Этот IO создаётся только при выполнении внешнего эффекта
      IO.pure("Defer: вычисление построено позже")
        .asInstanceOf[F[String]]
    }

  // MonadCancel: действие при отмене
  def cancelExample: IO[String] =
    for
      fiber <-
        IO.never[Unit]
          .onCancel(IO.println("MonadCancel: Fiber отменена"))
          .start

      _ <- IO.sleep(100.millis)
      _ <- fiber.cancel
    yield "MonadCancel: отмена завершена"

  // Spawn: запуск отдельной Fiber
  def spawnExample[F[_]](
                          using F: Spawn[F]
                        ): F[String] =
    for
      fiber <- F.start(F.pure("результат Fiber"))
      outcome <- fiber.join
    yield s"Spawn: результат Fiber = $outcome"

  // Concurrent: безопасное состояние и сигнал
  def concurrentExample[F[_]](
                               using F: Concurrent[F]
                             ): F[String] =
    for
      counter <- Ref.of[F, Int](0)
      _ <- counter.update(_ + 1)
      value <- counter.get

      signal <- Deferred[F, String]
      _ <- signal.complete(s"Concurrent: counter = $value")
      result <- signal.get
    yield result

  // Temporal: неблокирующий sleep
  def temporalExample[F[_]](
                             using F: Temporal[F]
                           ): F[String] =
    F.sleep(200.millis) *>
      F.pure("Temporal: Fiber проснулась")

  // Sync: обычный синхронный side effect
  def syncExample[F[_]](
                         using F: Sync[F]
                       ): F[String] =
    F.delay {
      val thread = Thread.currentThread().getName
      s"Sync: выполняемся в потоке $thread"
    }

  // Sync.blocking: честно маркируем блокирующую операцию
  def blockingExample[F[_]](
                             using F: Sync[F]
                           ): F[String] =
    F.blocking {
      Thread.sleep(100)
      "Sync.blocking: блокирующая операция завершена"
    }

  // Async: callback API превращается в F[A]
  def asyncExample[F[_]](
                          using F: Async[F]
                        ): F[String] =
    F.async_[String] { callback =>
      val worker = new Thread(() =>
        try
          Thread.sleep(100)

          callback(
            Right("Async: callback преобразован в эффект")
          )
        catch
          case error: Throwable =>
            callback(Left(error))
      )

      worker.setDaemon(true)
      worker.start()
    }

  // Clock: получение времени внутри эффекта
  def clockExample[F[_]](
                          using F: Clock[F],
                          M: cats.Monad[F]
                        ): F[String] =
    F.monotonic.map { time =>
      s"Clock: monotonic = $time"
    }

  // Unique: уникальный токен
  def uniqueExample[F[_]](
                           using F: Unique[F],
                           M: cats.Monad[F]
                         ): F[String] =
    F.unique.map { token =>
      s"Unique: token = $token"
    }

  private def printSection(
                            title: String,
                            program: IO[String]
                          ): IO[Unit] =
    for
      _ <- IO.println(s"\n===== $title =====")
      result <- program
      _ <- IO.println(result)
    yield ()

  override val run: IO[Unit] =
    for
      _ <- printSection(
        "MonadError",
        monadErrorExample[IO]
      )

      _ <- printSection(
        "MonadCancel",
        cancelExample
      )

      _ <- printSection(
        "Spawn",
        spawnExample[IO]
      )

      _ <- printSection(
        "Concurrent",
        concurrentExample[IO]
      )

      _ <- printSection(
        "Temporal",
        temporalExample[IO]
      )

      _ <- printSection(
        "Sync",
        syncExample[IO]
      )

      _ <- printSection(
        "Sync.blocking",
        blockingExample[IO]
      )

      _ <- printSection(
        "Async",
        asyncExample[IO]
      )

      _ <- printSection(
        "Clock",
        clockExample[IO]
      )

      _ <- printSection(
        "Unique",
        uniqueExample[IO]
      )
    yield ()
