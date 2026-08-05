//> using scala "3.3.6"
//> using dep "org.typelevel::cats-effect:3.7.0"

import cats.effect.*
import cats.effect.kernel.Outcome
import cats.syntax.all.*

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.concurrent.duration.*

/*
  Один live-файл по темам:
  1. IO
  2. Resource
  3. Ref
  4. Deferred
  5. Fiber
  6. Blocking

  Запуск:
    scala-cli run CatsEffectLive.scala
*/

object CatsEffectLive extends IOApp.Simple:

  // ===========================================================================
  // Вспомогательный вывод
  // ===========================================================================

  private def section(title: String): IO[Unit] =
    IO.println(
      s"""
         |========================================================================
         |$title
         |========================================================================
         |""".stripMargin
    )

  private def line(text: String): IO[Unit] =
    IO.println(s"  $text")

  // ===========================================================================
  // 1. IO
  // ===========================================================================

  private val ioLazinessExample: IO[Unit] =
    for
      _ <- section("1.1 IO: ленивое описание эффекта")

      effect = IO.println("Эффект реально выполнился")

      _ <- line("Сначала мы только создали значение IO[Unit]")
      _ <- effect
      _ <- effect
    yield ()

  private val ioCompositionExample: IO[Unit] =
    for
      _ <- section("1.2 IO: map, flatMap и for-comprehension")

      number <- IO.delay(40)
      result <- IO.delay(number + 2)
      text   <- IO.pure(s"Результат: $result")

      _ <- line(text)
    yield ()

  private val ioErrorExample: IO[Unit] =
    for
      _ <- section("1.3 IO: ошибка и обработка ошибки")

      failed: IO[Int] =
        IO.raiseError(new RuntimeException("Учебная ошибка"))

      recovered <- failed.handleErrorWith: error =>
        line(s"Перехватили: ${error.getMessage}").as(0)

      _ <- line(s"После восстановления получили: $recovered")
    yield ()

  private val ioAttemptExample: IO[Unit] =
    for
      _ <- section("1.4 IO: attempt превращает ошибку в Either")

      result <- IO.raiseError[Int](new IllegalArgumentException("bad value")).attempt

      _ <- result match
        case Right(value) => line(s"Успех: $value")
        case Left(error)  => line(s"Ошибка как значение: ${error.getMessage}")
    yield ()

  private val ioGuaranteeExample: IO[Unit] =
    for
      _ <- section("1.5 IO: guarantee выполняет финализатор")

      _ <- (
        line("Основное действие") *>
          IO.raiseError[Unit](new RuntimeException("boom"))
        ).guarantee(
        line("Финализатор выполнился")
      ).attempt
    yield ()

  // ===========================================================================
  // 2. Resource
  // ===========================================================================

  private def temporaryFile: Resource[IO, Path] =
    Resource.make(
      IO.blocking:
        val path = Files.createTempFile("cats-effect-live-", ".txt")
        Files.writeString(
          path,
          "Первая строка\nВторая строка\n",
          StandardCharsets.UTF_8
        )
        println(s"[acquire file] Создан временный файл: $path")
        path
    )(path =>
      IO.blocking:
        Files.deleteIfExists(path)
        println(s"[release file] Файл удалён: $path")
    )

  private def reader(path: Path): Resource[IO, BufferedReader] =
    Resource.make(
      IO.blocking:
        println("[acquire reader] Открываем BufferedReader")
        Files.newBufferedReader(path, StandardCharsets.UTF_8)
    )(bufferedReader =>
      IO.blocking:
        println("[release reader] Закрываем BufferedReader")
        bufferedReader.close()
    )

  private val resourceMakeUseExample: IO[Unit] =
    for
      _ <- section("2.1 Resource.make и use")

      _ <- temporaryFile.use: path =>
        line(s"Используем ресурс внутри use: ${path.getFileName}")
    yield ()

  private val resourceCompositionExample: IO[Unit] =
    for
      _ <- section("2.2 Композиция нескольких Resource")

      composed: Resource[IO, (Path, BufferedReader)] =
        for
          path           <- temporaryFile
          bufferedReader <- reader(path)
        yield (path, bufferedReader)

      _ <- composed.use: (path, bufferedReader) =>
        for
          firstLine <- IO.blocking(bufferedReader.readLine())
          _         <- line(s"${path.getFileName}: $firstLine")
        yield ()
    yield ()

  private val resourceEvalExample: IO[Unit] =
    for
      _ <- section("2.3 Resource.eval для эффекта без release")

      resource =
        for
          config <- Resource.eval(IO.pure("production"))
          path   <- temporaryFile
        yield (config, path)

      _ <- resource.use: (config, path) =>
        line(s"config=$config, file=${path.getFileName}")
    yield ()

  private val resourceErrorSafetyExample: IO[Unit] =
    for
      _ <- section("2.4 Resource освобождается при ошибке")

      result <- temporaryFile
        .use: _ =>
          line("Внутри use сейчас произойдёт ошибка") *>
            IO.raiseError[Unit](new RuntimeException("Ошибка внутри use"))
        .attempt

      _ <- line(s"Результат use: $result")
    yield ()

  private val resourceAllocatedExample: IO[Unit] =
    for
      _ <- section("2.5 allocated: ручное получение resource и release")

      allocated <- temporaryFile.allocated
      (path, release) = allocated

      _ <- line(s"Ресурс получен вручную: ${path.getFileName}")
      _ <- release
      _ <- line("release вызван вручную; обычно лучше использовать use")
    yield ()

  // ===========================================================================
  // 3. Ref
  // ===========================================================================

  private val refBasicOperationsExample: IO[Unit] =
    for
      _ <- section("3.1 Ref: get, set, update")

      ref <- Ref.of[IO, Int](10)

      initial <- ref.get
      _       <- line(s"Начальное значение: $initial")

      _        <- ref.set(20)
      afterSet <- ref.get
      _        <- line(s"После set(20): $afterSet")

      _           <- ref.update(_ + 1)
      afterUpdate <- ref.get
      _           <- line(s"После update(_ + 1): $afterUpdate")
    yield ()

  private val refReturningOperationsExample: IO[Unit] =
    for
      _ <- section("3.2 Ref: updateAndGet, getAndUpdate, getAndSet")

      ref <- Ref.of[IO, Int](100)

      newValue <- ref.updateAndGet(_ + 1)
      _        <- line(s"updateAndGet вернул новое значение: $newValue")

      oldValue <- ref.getAndUpdate(_ + 10)
      _        <- line(s"getAndUpdate вернул старое значение: $oldValue")

      beforeSet <- ref.getAndSet(999)
      current   <- ref.get

      _ <- line(s"getAndSet вернул: $beforeSet")
      _ <- line(s"Сейчас внутри Ref: $current")
    yield ()

  private val refModifyExample: IO[Unit] =
    for
      _ <- section("3.3 Ref.modify: обновление и отдельный результат")

      ref <- Ref.of[IO, List[String]](List("A", "B", "C"))

      taken <- ref.modify:
        case head :: tail => (tail, Some(head))
        case Nil          => (Nil, None)

      remaining <- ref.get

      _ <- line(s"Забрали: $taken")
      _ <- line(s"Осталось: $remaining")
    yield ()

  private def incrementMany(ref: Ref[IO, Int], times: Int): IO[Unit] =
    List.fill(times)(ref.update(_ + 1)).sequence_

  private val refConcurrencyExample: IO[Unit] =
    for
      _ <- section("3.4 Ref: атомарное конкурентное обновление")

      counter <- Ref.of[IO, Int](0)

      fiberA <- incrementMany(counter, 1000).start
      fiberB <- incrementMany(counter, 1000).start

      _ <- fiberA.joinWithNever
      _ <- fiberB.joinWithNever

      result <- counter.get
      _      <- line(s"Итог после 2000 обновлений: $result")
    yield ()

  private val refGetSetRaceExplanation: IO[Unit] =
    for
      _ <- section("3.5 get + set не образуют одну атомарную операцию")

      ref <- Ref.of[IO, Int](0)

      unsafeIncrement =
        for
          current <- ref.get
          _       <- IO.cede
          _       <- ref.set(current + 1)
        yield ()

      _ <- (unsafeIncrement, unsafeIncrement).parTupled
      result <- ref.get

      _ <- line(s"Результат может быть 1 вместо 2: $result")
      _ <- line("Для корректности нужно ref.update(_ + 1)")
    yield ()

  // ===========================================================================
  // 4. Deferred
  // ===========================================================================

  private val deferredBasicExample: IO[Unit] =
    for
      _ <- section("4.1 Deferred: get ждёт, complete заполняет один раз")

      deferred <- Deferred[IO, String]

      waiter <- (
        line("Consumer ждёт") *>
          deferred.get.flatMap(value => line(s"Consumer получил: $value"))
        ).start

      _ <- IO.sleep(300.millis)
      firstComplete  <- deferred.complete("готово")
      secondComplete <- deferred.complete("другое значение")

      _ <- waiter.joinWithNever

      _ <- line(s"Первый complete: $firstComplete")
      _ <- line(s"Второй complete: $secondComplete")
    yield ()

  private val deferredMultipleConsumersExample: IO[Unit] =
    for
      _ <- section("4.2 Один Deferred — несколько ожидающих fibers")

      signal <- Deferred[IO, Int]

      consumer = (name: String) =>
        signal.get.flatMap(value => line(s"$name получил $value"))

      fibers <- List("A", "B", "C").traverse(name => consumer(name).start)

      _ <- IO.sleep(200.millis)
      _ <- signal.complete(42)
      _ <- fibers.traverse_(_.joinWithNever)
    yield ()

  private val deferredErrorExample: IO[Unit] =
    for
      _ <- section("4.3 Передача успеха или ошибки через Either")

      result <- Deferred[IO, Either[Throwable, Int]]

      producer <- (
        IO.raiseError[Int](new RuntimeException("Ошибка producer"))
          .attempt
          .flatMap(result.complete)
          .void
        ).start

      received <- result.get
      _ <- received match
        case Right(value) => line(s"Успех: $value")
        case Left(error)  => line(s"Получили ошибку: ${error.getMessage}")

      _ <- producer.joinWithNever
    yield ()

  private val deferredWinnerExample: IO[Unit] =
    for
      _ <- section("4.4 Несколько producers: первый complete побеждает")

      winner <- Deferred[IO, String]

      slow =
        IO.sleep(400.millis) *>
          winner.complete("slow").flatMap(ok => line(s"slow complete=$ok"))

      fast =
        IO.sleep(100.millis) *>
          winner.complete("fast").flatMap(ok => line(s"fast complete=$ok"))

      fibers <- List(slow, fast).traverse(_.start)
      value  <- winner.get
      _      <- line(s"Победитель: $value")
      _      <- fibers.traverse_(_.joinWithNever)
    yield ()

  // ===========================================================================
  // 5. Fiber
  // ===========================================================================

  private val fiberStartJoinExample: IO[Unit] =
    for
      _ <- section("5.1 Fiber.start и join")

      child <- (
        line("Child: старт") *>
          IO.sleep(300.millis) *>
          line("Child: завершение")
        ).start

      _ <- line("Parent продолжает сразу после start")

      outcome <- child.join
      _       <- line(s"join вернул: $outcome")
    yield ()

  private val fiberCancelExample: IO[Unit] =
    for
      _ <- section("5.2 Fiber.cancel и финализатор")

      child <- (
        line("Child: начал долгую работу") *>
          IO.sleep(10.seconds) *>
          line("Child: нормальное завершение")
        ).guarantee(
        line("Child: финализатор")
      ).start

      _ <- IO.sleep(300.millis)
      _ <- line("Parent вызывает cancel")
      _ <- child.cancel

      outcome <- child.join
      _       <- line(s"После cancel outcome=$outcome")
    yield ()

  private val fiberOutcomeExample: IO[Unit] =
    for
      _ <- section("5.3 Outcome: Succeeded, Errored, Canceled")

      successFiber <- IO.pure(42).start
      errorFiber   <- IO.raiseError[Int](new RuntimeException("fiber error")).start
      cancelFiber  <- IO.never[Int].start

      _ <- cancelFiber.cancel

      success <- successFiber.join
      error   <- errorFiber.join
      cancel  <- cancelFiber.join

      _ <- printOutcome("success", success)
      _ <- printOutcome("error", error)
      _ <- printOutcome("cancel", cancel)
    yield ()

  private def printOutcome[A](
                               name: String,
                               outcome: Outcome[IO, Throwable, A]
                             ): IO[Unit] =
    outcome match
      case Outcome.Succeeded(result) =>
        result.flatMap(value => line(s"$name: Succeeded($value)"))

      case Outcome.Errored(error) =>
        line(s"$name: Errored(${error.getMessage})")

      case Outcome.Canceled() =>
        line(s"$name: Canceled")

  private val fiberParTupledExample: IO[Unit] =
    for
      _ <- section("5.4 parTupled: высокоуровневая конкурентная композиция")

      left =
        IO.sleep(300.millis) *>
          line("Левая ветка завершилась").as(10)

      right =
        IO.sleep(100.millis) *>
          line("Правая ветка завершилась").as(20)

      pair <- (left, right).parTupled
      _    <- line(s"Результаты: $pair")
    yield ()

  private val fiberRaceExample: IO[Unit] =
    for
      _ <- section("5.5 race: результат первой завершившейся операции")

      slow = IO.sleep(500.millis).as("slow")
      fast = IO.sleep(100.millis).as("fast")

      result <- slow.race(fast)
      _      <- line(s"race вернул: $result")
    yield ()

  private val fiberJoinOtherExample: IO[Unit] =
    for
      _ <- section("5.6 Fiber A запускает B и ждёт Fiber C")

      fiberC <- (
        line("Fiber C: старт") *>
          IO.sleep(200.millis) *>
          line("Fiber C: конец")
        ).start

      fiberB <- (
        line("Fiber B: старт") *>
          IO.sleep(400.millis) *>
          line("Fiber B: конец")
        ).start

      _ <- line("Fiber A: ждёт C")
      _ <- fiberC.joinWithNever
      _ <- line("Fiber A: C завершился, продолжаем")

      _ <- fiberB.joinWithNever
      _ <- line("Fiber A: B тоже завершился")
    yield ()

  // ===========================================================================
  // 6. Blocking
  // ===========================================================================

  private val semanticBlockingExample: IO[Unit] =
    for
      _ <- section("6.1 IO.sleep: семантическая блокировка fiber")

      sleeper =
        for
          before <- IO(Thread.currentThread().getName)
          _      <- line(s"До sleep поток: $before")
          _      <- IO.sleep(200.millis)
          after  <- IO(Thread.currentThread().getName)
          _      <- line(s"После sleep поток: $after")
        yield ()

      worker =
        List.range(1, 4).traverse_(n =>
          IO.sleep(50.millis) *> line(s"Другой fiber работает: шаг $n")
        )

      _ <- (sleeper, worker).parTupled
    yield ()

  private val physicalBlockingExample: IO[Unit] =
    for
      _ <- section("6.2 IO.blocking: физически блокирующий вызов")

      threadName <- IO.blocking:
        Thread.sleep(200)
        Thread.currentThread().getName

      _ <- line(s"Blocking-вызов выполнен на потоке: $threadName")
    yield ()

  private val blockingFileExample: IO[Unit] =
    for
      _ <- section("6.3 Файловый ввод-вывод через IO.blocking")

      path <- IO.blocking:
        val path = Files.createTempFile("blocking-demo-", ".txt")
        Files.writeString(path, "blocking example", StandardCharsets.UTF_8)
        path

      text <- IO.blocking:
        Files.readString(path, StandardCharsets.UTF_8)

      _ <- line(s"Прочитано: $text")
      _ <- IO.blocking(Files.deleteIfExists(path)).void
    yield ()

  // ===========================================================================
  // Запуск всего live
  // ===========================================================================

  override val run: IO[Unit] =
    List(
      ioLazinessExample,
      ioCompositionExample,
      ioErrorExample,
      ioAttemptExample,
      ioGuaranteeExample,

      resourceMakeUseExample,
      resourceCompositionExample,
      resourceEvalExample,
      resourceErrorSafetyExample,
      resourceAllocatedExample,

      refBasicOperationsExample,
      refReturningOperationsExample,
      refModifyExample,
      refConcurrencyExample,
      refGetSetRaceExplanation,

      deferredBasicExample,
      deferredMultipleConsumersExample,
      deferredErrorExample,
      deferredWinnerExample,

      fiberStartJoinExample,
      fiberCancelExample,
      fiberOutcomeExample,
      fiberParTupledExample,
      fiberRaceExample,
      fiberJoinOtherExample,

      semanticBlockingExample,
      physicalBlockingExample,
      blockingFileExample
    ).sequence_.handleErrorWith: error =>
      IO.println(
        s"""
           |LIVE завершился с ошибкой:
           |${error.getClass.getSimpleName}: ${error.getMessage}
           |""".stripMargin
      ) *> IO.raiseError(error)
