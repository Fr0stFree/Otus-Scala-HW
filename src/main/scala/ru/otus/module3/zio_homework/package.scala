package ru.otus.module3

import scala.language.postfixOps
import zio._
import ru.otus.module3.zio_homework.config.Configuration
import ru.otus.module3.zio_homework.config._
import java.util.concurrent.TimeUnit

package object zio_homework {
  /**
   * 1.
   * Используя сервисы Random и Console, напишите консольную ZIO программу которая будет предлагать пользователю угадать число от 1 до 3
   * и печатать в консоль угадал или нет. Подумайте, на какие наиболее простые эффекты ее можно декомпозировать.
   */
  private def analyzeUserGuess(guess: Int, number: Int): String =
    if (guess == number)
      "You guessed it!"
    else
      s"Wrong! The number was $number"

  lazy val guessProgram = {
    for {
      number <- Random.nextIntBetween(1, 4)
      _ <- Console.printLine("Guess a number between 1 and 3:")
      input <- Console.readLine
      guess = input.toIntOption.getOrElse(0)
      _ <- Console.printLine(analyzeUserGuess(guess, number))
    } yield ()
  }

  /**
   * 2. реализовать функцию doWhile (общего назначения), которая будет выполнять эффект до тех пор, пока его значение в условии не даст true
   * 
   */

  def doWhile[R, E, A](effect: ZIO[R, E, A])(condition: A => Boolean): ZIO[R, E, A] =
    effect.flatMap { value =>
      if (condition(value)) ZIO.succeed(value)
      else doWhile(effect)(condition)
    }

  /**
   * 3. Реализовать метод, который безопасно прочитает конфиг из переменных окружения, а в случае ошибки вернет дефолтный конфиг
   * и выведет его в консоль
   * Используйте эффект "Configuration.config" из пакета config
   */


  def loadConfigOrDefault =
    Configuration
      .config
      .catchAll { error =>
        val defaultConfig = AppConfig("127.0.0.1", "8000")
        Console
          .printLine(s"Failed to load config due to an error: $error")
          .as(defaultConfig)
    }


  /**
   * 4. Следуйте инструкциям ниже для написания 2-х ZIO программ,
   * обратите внимание на сигнатуры эффектов, которые будут у вас получаться,
   * на изменение этих сигнатур
   */


  /**
   * 4.1 Создайте эффект, который будет возвращать случайным образом выбранное число от 0 до 10 спустя 1 секунду
   * Используйте сервис zio Random
   */
  lazy val eff: UIO[Int] = {
    for {
      number <- Random.nextIntBetween(0, 11)
      _ <- ZIO.sleep(1.second)
    } yield number
  }

  /**
   * 4.2 Создайте коллукцию из 10 выше описанных эффектов (eff)
   */
  lazy val effects: Seq[UIO[Int]] = Seq.fill(10)(eff)

  
  /**
   * 4.3 Напишите программу которая вычислит сумму элементов коллекции "effects",
   * напечатает ее в консоль и вернет результат, а также залогирует затраченное время на выполнение,
   * можно использовать ф-цию printEffectRunningTime, которую мы разработали на занятиях
   */

  lazy val app = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      results <- ZIO.collectAll(effects)
      sum = results.sum
      _ <- Console.printLine(s"Sum: $sum")
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- Console.printLine(s"Running time: ${endTime - startTime} ms")
    } yield sum
  }


  /**
   * 4.4 Усовершенствуйте программу 4.3 так, чтобы минимизировать время ее выполнения
   */

  lazy val appSpeedUp = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      results <- ZIO.collectAllPar(effects)
      sum = results.sum
      _ <- Console.printLine(s"Sum: $sum")
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <- Console.printLine(s"Running time: ${endTime - startTime} ms")
    } yield sum
  }


  /**
   * 5. Оформите ф-цию printEffectRunningTime разработанную на занятиях в отдельный сервис, так чтобы ее
   * можно было использовать аналогично zio.Console.printLine например
   */
  trait EffectTimer {
    def printEffectRunningTime[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A]
  }

  final class EffectTimerLive extends EffectTimer {

    override def printEffectRunningTime[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, A] =
      for {
        start <- Clock.currentTime(TimeUnit.MILLISECONDS)
        result <- effect
        end <- Clock.currentTime(TimeUnit.MILLISECONDS)
        _ <- Console
          .printLine(s"Running time: ${end - start} ms")
          .orDie
      } yield result
  }

  object EffectTimer {
    def printEffectRunningTime[R, E, A](effect: ZIO[R, E, A]): ZIO[R with EffectTimer, E, A] =
      ZIO.serviceWithZIO[EffectTimer] { timer =>
        timer.printEffectRunningTime(effect)
      }

    val live: ULayer[EffectTimer] = ZLayer.succeed(new EffectTimerLive)
  }


   /**
     * 6.
     * Воспользуйтесь написанным сервисом, чтобы создать эффект, который будет логировать время выполнения программы из пункта 4.3
     *
     * 
     */

  lazy val appWithTimeLogg = EffectTimer.printEffectRunningTime(app)

  /**
    * 
    * Подготовьте его к запуску и затем запустите воспользовавшись ZioHomeWorkApp
    */

  lazy val runApp = appWithTimeLogg.provide(EffectTimer.live)

}
