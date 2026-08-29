package module5

import zio.*
import zio.stream.*

object ZStreamErrors extends ZIOAppDefault {

  case class Payment(
                      id: Int,
                      amount: Int
                    )

  val payments =
    ZStream(
      Payment(1, 100),
      Payment(2, 200),
      Payment(3, -500), // плохое событие
      Payment(4, 400),
      Payment(5, 500)
    )

  def process(payment: Payment): IO[String, String] =
    if payment.amount < 0 then
      ZIO.fail(
        s"Payment ${payment.id}: negative amount"
      )
    else
      ZIO.succeed(
        s"Payment ${payment.id}: ${payment.amount} ₽"
      )

  override def run =
    payments
      .mapZIO(process)
      .tap(result =>
        Console.printLine(s"OK: $result").orDie
      )
      .catchAll(error =>
        ZStream.succeed(s"ERROR: $error")
      )
      .runForeach(Console.printLine(_))
}