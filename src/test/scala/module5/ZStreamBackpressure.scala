package module5

import zio.*
import zio.stream.*

import java.util.concurrent.TimeUnit

object ZStreamBackpressure extends ZIOAppDefault {

  def now =
    Clock.currentTime(TimeUnit.MILLISECONDS)

  val producer =
    ZStream
      .iterate(1)(_ + 1)
      .schedule(Schedule.spaced(100.millis))
      .tap { n =>
        Console.printLine(
          s"PRODUCER -> $n"
        ).orDie
      }

  def slowConsumer(n: Int) =
    for {
      _ <- ZIO.sleep(1.second)

      _ <- Console.printLine(
        s"             CONSUMER <- $n"
      )
    } yield ()

  override def run =
    producer
      .take(10)
      .mapZIO(slowConsumer)
      .runDrain
}