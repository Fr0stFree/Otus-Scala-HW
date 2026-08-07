package ru.otus.module3.fs2

import cats.effect.{IO, IOApp}
import fs2.{Chunk, Stream}

object Fs2Example extends IOApp.Simple {

  // --------------------------------------------------
  // 1. Простой чистый stream
  // --------------------------------------------------

  val numbers: Stream[fs2.Pure, Int] =
    Stream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

  // --------------------------------------------------
  // 2. map
  // --------------------------------------------------

  val multiplied: Stream[fs2.Pure, Int] =
    numbers.map(x => x * 10)

  // --------------------------------------------------
  // 3. filter
  // --------------------------------------------------

  val filtered: Stream[fs2.Pure, Int] =
    multiplied.filter(x => x >= 50)

  // --------------------------------------------------
  // 4. Можно записать короче
  // --------------------------------------------------

  val processed: Stream[fs2.Pure, Int] =
    numbers
      .map(x => x * 10)
      .filter(x => x >= 50)

  // --------------------------------------------------
  // 5. Pure stream превращаем в IO stream
  // --------------------------------------------------

  val ioStream: Stream[IO, Int] =
    processed.covary[IO]

  // --------------------------------------------------
  // 6. Эффектная обработка
  // --------------------------------------------------

  val printed: Stream[IO, Unit] =
    ioStream.evalMap { number =>
      IO.println(s"Получили число: $number")
    }

  // --------------------------------------------------
  // 7. Stream с эффектом,
  //    но сохраняем исходное значение
  // --------------------------------------------------

  val loggedNumbers: Stream[IO, Int] =
    ioStream.evalMap { number =>
      IO.println(s"Обрабатываем: $number")
        .as(number)
    }

  // --------------------------------------------------
  // 8. Ещё один map после эффекта
  // --------------------------------------------------

  val finalNumbers: Stream[IO, Int] =
    loggedNumbers.map { number =>
      number + 1
    }

  // --------------------------------------------------
  // 9. Работа с Chunk явно
  // --------------------------------------------------

  val chunk: Chunk[Int] =
    Chunk(100, 200, 300, 400)

  val chunkStream: Stream[fs2.Pure, Int] =
    Stream.chunk(chunk)

  // --------------------------------------------------
  // 10. Объединение двух stream
  // --------------------------------------------------

  val allNumbers: Stream[IO, Int] =
    finalNumbers ++ chunkStream.covary[IO]

  // --------------------------------------------------
  // 11. Основная программа
  // --------------------------------------------------

  override def run: IO[Unit] = {

    val program: IO[Unit] =
      for {
        _ <- IO.println("=== Исходные числа ===")

        original <- numbers.covary[IO].compile.toList
        _ <- IO.println(original)

        _ <- IO.println("\n=== После map ===")

        afterMap <- multiplied.covary[IO].compile.toList
        _ <- IO.println(afterMap)

        _ <- IO.println("\n=== После filter ===")

        afterFilter <- filtered.covary[IO].compile.toList
        _ <- IO.println(afterFilter)

        _ <- IO.println("\n=== evalMap / эффекты ===")

        _ <- printed.compile.drain

        _ <- IO.println("\n=== Итоговый stream ===")

        result <- finalNumbers.compile.toList
        _ <- IO.println(result)

        _ <- IO.println("\n=== Chunk ===")

        chunkResult <- chunkStream.covary[IO].compile.toList
        _ <- IO.println(chunkResult)

        _ <- IO.println("\n=== Объединённый stream ===")

        all <- allNumbers.compile.toList
        _ <- IO.println(all)

      } yield ()

    program
  }
}