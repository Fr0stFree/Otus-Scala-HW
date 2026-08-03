package ru.otus.module3.cats
// CatsEffectHierarchyExamples.scala
//
// Вариант A — Scala CLI (IntelliJ IDEA также понимает directives при установленном плагине):
//> using scala "3.3.6"
//> using dep "org.typelevel::cats-effect:3.7.0"
//
// Вариант B — обычный sbt-проект:
// libraryDependencies += "org.typelevel" %% "cats-effect" % "3.7.0"
//
// Запуск: запустите объект CatsEffectHierarchyExamples как обычное приложение.

import cats.{Defer, Monad, MonadError}
import cats.syntax.all.*
import cats.effect.*
import cats.effect.kernel.*

import scala.concurrent.duration.*

object CatsEffectHierarchyExamples extends IOApp.Simple:

  /*
   * ИЕРАРХИЯ, КОТОРУЮ ПОКАЗЫВАЮТ ПРИМЕРЫ
   *
   * Cats:
   *   MonadError
   *   Defer
   *
   * Cats Effect:
   *   MonadCancel -> Spawn -> Concurrent -> Temporal -> Async
   *   Defer -> Sync ------------------------------^
   *
   * Дополнительные небольшие type classes:
   *   Clock, Unique
   *
   * Важно:
   *   type class описывает возможности F[_];
   *   IO — конкретный эффект, для которого существуют реализации этих type classes.
   */

  // ---------------------------------------------------------------------------
  // 1. Defer — отложить построение вычисления
  // ---------------------------------------------------------------------------

  def deferExample[F[_]](using D: Defer[F], M: Monad[F]): F[String] =
    D.defer {
      // Выражение внутри defer строится только тогда,
      // когда эффект действительно будет вычисляться.
      M.pure("Defer: вычисление было отложено")
    }

  // ---------------------------------------------------------------------------
  // 2. MonadError — поднять и обработать типизированную ошибку
  // ---------------------------------------------------------------------------

  def monadErrorExample[F[_]](using F: MonadError[F, Throwable]): F[String] =
    val failed: F[String] =
      F.raiseError(new RuntimeException("ошибка из MonadError"))

    failed.handleError(error => s"MonadError: обработано: ${error.getMessage}")

  // ---------------------------------------------------------------------------
  // 3. MonadCancel — гарантированный финализатор и безопасность ресурсов
  // ---------------------------------------------------------------------------

  def monadCancelExample[F[_]](using F: MonadCancel[F, Throwable]): F[List[String]] =
    val acquire: F[String] =
      F.pure("ресурс")

    def use(resource: String): F[List[String]] =
      F.pure(List(s"MonadCancel: используем $resource"))

    def release(resource: String): F[Unit] =
      // Финализатор будет запущен при успехе, ошибке и отмене.
      F.unit

    F.bracket(acquire)(use)(release)

  // ---------------------------------------------------------------------------
  // 4. Spawn — запустить Fiber и управлять его жизненным циклом
  // ---------------------------------------------------------------------------

  def spawnExample[F[_]](using F: Spawn[F]): F[String] =
    for
      fiber <- F.start(F.never[Unit])
      _     <- fiber.cancel
    yield "Spawn: Fiber создан и отменён"

  // ---------------------------------------------------------------------------
  // 5. Concurrent — Ref и Deferred для конкурентного состояния
  // ---------------------------------------------------------------------------

  def concurrentExample[F[_]](using F: Concurrent[F]): F[String] =
    for
      counter <- Ref.of[F, Int](0)
      _       <- counter.update(_ + 1)
      value   <- counter.get

      signal  <- Deferred[F, String]
      _       <- signal.complete(s"Concurrent: Ref содержит $value")
      result  <- signal.get
    yield result

  // ---------------------------------------------------------------------------
  // 6. Temporal — неблокирующий sleep и операции, связанные со временем
  // ---------------------------------------------------------------------------

  def temporalExample[F[_]](using F: Temporal[F]): F[String] =
    F.sleep(150.millis) *>
      F.pure("Temporal: Fiber уснул без блокировки JVM-потока")

  // ---------------------------------------------------------------------------
  // 7. Sync — синхронный FFI: безопасно поместить обычный side effect в F
  // ---------------------------------------------------------------------------

  def syncExample[F[_]](using F: Sync[F]): F[String] =
    F.delay {
      // Этот код не выполняется в момент создания F[String].
      val threadName = Thread.currentThread().getName
      s"Sync: side effect выполнен в потоке $threadName"
    }

  // Для блокирующих операций существует отдельный конструктор:
  def blockingExample[F[_]](using F: Sync[F]): F[String] =
    F.blocking {
      Thread.sleep(30)
      "Sync.blocking: блокирующая операция завершена"
    }

  // ---------------------------------------------------------------------------
  // 8. Async — асинхронный FFI: callback API превращается в F[A]
  // ---------------------------------------------------------------------------

  def asyncExample[F[_]](using F: Async[F]): F[String] =
    F.async_[String] { callback =>
      val worker = new Thread(() =>
        try
          Thread.sleep(50)
          callback(Right("Async: callback преобразован в F[String]"))
        catch
          case error: Throwable => callback(Left(error))
      )

      worker.setDaemon(true)
      worker.start()
    }

  // ---------------------------------------------------------------------------
  // 9. Clock — монотонное и реальное время
  // ---------------------------------------------------------------------------

  def clockExample[F[_]](using F: Clock[F], M: Monad[F]): F[String] =
    F.monotonic.map(duration => s"Clock: monotonic = $duration")

  // ---------------------------------------------------------------------------
  // 10. Unique — создание уникального токена внутри эффекта
  // ---------------------------------------------------------------------------

  def uniqueExample[F[_]](using F: Unique[F], M: Monad[F]): F[String] =
    F.unique.map(token => s"Unique: получен токен $token")

  // ---------------------------------------------------------------------------
  // 11. Принцип наименьшей силы
  // ---------------------------------------------------------------------------

  // Функции следует давать минимальное ограничение.
  // Для sleep достаточно Temporal — требовать Async было бы избыточно.
  def waitAndReturn[F[_]: Temporal](value: String): F[String] =
    Temporal[F].sleep(50.millis).as(value)

  // Для помещения обычного side effect в F достаточно Sync.
  def readSystemProperty[F[_]: Sync](name: String): F[Option[String]] =
    Sync[F].delay(Option(System.getProperty(name)))

  // Для callback API нужен Async.
  def fromCallback[F[_]: Async]: F[Int] =
    Async[F].async_ { callback =>
      callback(Right(42))
    }

  // ---------------------------------------------------------------------------
  // Запуск всех примеров на конкретном эффекте IO
  // ---------------------------------------------------------------------------

  private def section(title: String): IO[Unit] =
    IO.println(s"\n===== $title =====")

  override val run: IO[Unit] =
    for
      _ <- section("Defer")
      value1 <- deferExample[IO]
      _ <- IO.println(value1)

      _ <- section("MonadError")
      value2 <- monadErrorExample[IO]
      _ <- IO.println(value2)

      _ <- section("MonadCancel")
      value3 <- monadCancelExample[IO]
      _ <- value3.traverse_(IO.println)

      _ <- section("Spawn")
      value4 <- spawnExample[IO]
      _ <- IO.println(value4)

      _ <- section("Concurrent")
      value5 <- concurrentExample[IO]
      _ <- IO.println(value5)

      _ <- section("Temporal")
      value6 <- temporalExample[IO]
      _ <- IO.println(value6)

      _ <- section("Sync")
      value7 <- syncExample[IO]
      _ <- IO.println(value7)
      value8 <- blockingExample[IO]
      _ <- IO.println(value8)

      _ <- section("Async")
      value9 <- asyncExample[IO]
      _ <- IO.println(value9)

      _ <- section("Clock")
      value10 <- clockExample[IO]
      _ <- IO.println(value10)

      _ <- section("Unique")
      value11 <- uniqueExample[IO]
      _ <- IO.println(value11)

      _ <- section("Принцип наименьшей силы")
      value12 <- waitAndReturn[IO]("Для ожидания достаточно Temporal")
      _ <- IO.println(value12)

      javaVersion <- readSystemProperty[IO]("java.version")
      _ <- IO.println(s"Версия Java: ${javaVersion.getOrElse("неизвестна")}")

      answer <- fromCallback[IO]
      _ <- IO.println(s"Результат callback: $answer")
    yield ()
