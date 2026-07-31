import cats.effect.{Async, Concurrent, IO, IOApp}
import cats.effect.std.Queue
import cats.effect.syntax.all.*
import cats.syntax.all.*

object Main extends IOApp.Simple {

  /*
   * ============================================================
   * 1. IO[A] — описание вычисления, которое вернёт A
   * ============================================================
   */

  val hello: IO[Unit] =
    IO.println("1. Hello из IO")

  /*
   * В момент создания hello печати ещё не происходит.
   *
   * hello — обычное значение типа IO[Unit].
   * Оно описывает действие, но не запускает его.
   */


  /*
   * ============================================================
   * 2. Отложенное побочное действие
   * ============================================================
   */

  def randomNumber: IO[Int] =
    IO {
      println("   Генерирую случайное число...")
      scala.util.Random.nextInt(100)
    }

  /*
   * Код внутри IO { ... } выполняется не при объявлении метода,
   * а только тогда, когда runtime Cats Effect запустит этот IO.
   */


  /*
   * ============================================================
   * 3. Ссылочная прозрачность
   * ============================================================
   */

  val reusableEffect: IO[Unit] =
    IO.println("   Выполняется reusableEffect")

  val referentialTransparencyDemo: IO[Unit] =
    for {
      _ <- IO.println("\n2. Повторное использование одного IO-значения:")

      // Одно и то же описание вычисления запускается два раза.
      _ <- reusableEffect
      _ <- reusableEffect
    } yield ()

  /*
   * reusableEffect можно:
   *
   * - сохранить в val;
   * - передать в функцию;
   * - вернуть из функции;
   * - использовать несколько раз;
   * - объединить с другими IO.
   *
   * При его создании побочный эффект не выполняется.
   */


  /*
   * ============================================================
   * 4. Ошибка встроена в IO и представлена Throwable
   * ============================================================
   */

  val failingComputation: IO[Int] =
    IO.raiseError(
      new IllegalStateException("Что-то пошло не так")
    )

  val errorDemo: IO[Unit] =
    for {
      _ <- IO.println("\n3. Встроенный канал ошибок Throwable:")

      result <- failingComputation.attempt

      _ <- result match {
        case Right(value) =>
          IO.println(s"   Успешный результат: $value")

        case Left(error) =>
          IO.println(
            s"   Перехвачена ошибка: " +
              s"${error.getClass.getSimpleName}: ${error.getMessage}"
          )
      }
    } yield ()

  /*
   * Тип failingComputation — IO[Int].
   *
   * В типе не написано:
   *
   * IO[IllegalStateException, Int]
   *
   * Канал ошибки уже встроен в IO, и ошибка представляется
   * значением типа Throwable.
   *
   * attempt превращает:
   *
   * IO[A]
   *
   * в:
   *
   * IO[Either[Throwable, A]]
   */


  /*
   * ============================================================
   * 5. Нет встроенного Environment
   * ============================================================
   */

  final class GreetingService(prefix: String) {

    def greet(name: String): IO[String] =
      IO.pure(s"$prefix, $name!")
  }

  /*
   * Зависимость prefix передаётся обычным способом Scala:
   * через параметр конструктора.
   *
   * Она не записывается внутри типа IO.
   */

  val environmentDemo: IO[Unit] = {
    val service =
      new GreetingService(prefix = "Добро пожаловать")

    for {
      _       <- IO.println("\n4. Зависимости без встроенного Env:")
      message <- service.greet("Алексей")
      _       <- IO.println(s"   $message")
    } yield ()
  }

  /*
   * Условное сравнение моделей:
   *
   * Cats Effect:
   *
   *   IO[A]
   *
   * ZIO:
   *
   *   ZIO[Any, Throwable, A]
   *
   * В ZIO это сокращается до:
   *
   *   zio.Task[A]
   *
   * Здесь знак соответствия смысловой, а не утверждение,
   * что IO и ZIO.Task являются одним Scala-типом.
   */


  /*
   * ============================================================
   * 6. Обобщённая конкурентная структура Queue[F, A]
   * ============================================================
   */

  def queueProgram[F[_]: Concurrent]: F[List[Int]] =
    for {
      queue <- Queue.bounded[F, Int](capacity = 2)

      /*
       * Производитель помещает числа в очередь.
       */
      producer =
        List(10, 20, 30)
          .traverse_(number => queue.offer(number))

      /*
       * Потребитель трижды извлекает элемент.
       *
       * Если очередь пуста, fiber будет приостановлен,
       * но JVM-поток не будет заблокирован.
       */
      consumer =
        List.fill(3)(queue.take).sequence

      /*
       * producer и consumer запускаются в отдельных fibers.
       */
      producerFiber <- producer.start
      consumerFiber <- consumer.start

      /*
       * Ждём завершения обоих fibers.
       */
      _       <- producerFiber.joinWithNever
      numbers <- consumerFiber.joinWithNever
    } yield numbers

  val queueDemo: IO[Unit] =
    for {
      _ <- IO.println("\n5. Обобщённая конкурентная Queue[IO, Int]:")

      numbers <- queueProgram[IO]

      _ <- IO.println(
        s"   Получены элементы: ${numbers.mkString(", ")}"
      )
    } yield ()

  /*
   * queueProgram не привязан к IO:
   *
   *   def queueProgram[F[_]: Concurrent]
   *
   * Он требует только доказательство Concurrent[F].
   *
   * Это означает:
   *
   *   программа работает с любым F,
   *   для которого существует реализация Concurrent[F].
   *
   * При вызове:
   *
   *   queueProgram[IO]
   *
   * компилятор находит реализацию Concurrent[IO],
   * предоставленную Cats Effect.
   */


  /*
   * ============================================================
   * 7. Async[F] и подключение callback API
   * ============================================================
   */

  /*
   * Представим стороннюю Java-библиотеку, которая не возвращает
   * IO, а сообщает результат через callback.
   */
  def legacyCallbackApi(
                         input: String,
                         callback: Either[Throwable, String] => Unit
                       ): Unit = {

    val thread = new Thread(
      () => {
        try {
          Thread.sleep(300)

          callback(
            Right(input.reverse)
          )
        } catch {
          case error: Throwable =>
            callback(Left(error))
        }
      }
    )

    thread.start()
  }

  /*
   * Async[F] позволяет превратить callback API в F[A].
   */
  def callbackToEffect[F[_]: Async](
                                     input: String
                                   ): F[String] =
    Async[F].async_ { callback =>
      legacyCallbackApi(input, callback)
    }

  val asyncDemo: IO[Unit] =
    for {
      _ <- IO.println("\n6. Преобразование callback API через Async:")

      result <- callbackToEffect[IO]("Scala")

      _ <- IO.println(
        s"   Результат внешнего callback API: $result"
      )
    } yield ()

  /*
   * callbackToEffect не знает внутреннего устройства IO.
   *
   * Он знает только, что для F существует Async[F].
   *
   * Async[F] предоставляет возможность включить внешнюю
   * асинхронную операцию в функциональную программу.
   */


  /*
   * ============================================================
   * 8. Композиция вычислений
   * ============================================================
   */

  val compositionDemo: IO[Unit] =
    for {
      _ <- IO.println("\n7. Последовательная композиция IO:")

      first  <- randomNumber
      second <- randomNumber

      sum = first + second

      _ <- IO.println(s"   Первое число: $first")
      _ <- IO.println(s"   Второе число: $second")
      _ <- IO.println(s"   Сумма: $sum")
    } yield ()

  /*
   * for-comprehension последовательно соединяет IO:
   *
   * 1. выполнить randomNumber;
   * 2. получить first;
   * 3. выполнить второй randomNumber;
   * 4. получить second;
   * 5. вычислить сумму;
   * 6. напечатать результаты.
   *
   * Весь compositionDemo при этом тоже является одним
   * значением типа IO[Unit].
   */


  /*
   * ============================================================
   * 9. Точка запуска всей программы
   * ============================================================
   */

  override val run: IO[Unit] =
    for {
      _ <- IO.println("=== Cats Effect: демонстрация IO ===")

      _ <- hello
      _ <- referentialTransparencyDemo
      _ <- errorDemo
      _ <- environmentDemo
      _ <- queueDemo
      _ <- asyncDemo
      _ <- compositionDemo

      _ <- IO.println("\n=== Программа успешно завершена ===")
    } yield ()
}