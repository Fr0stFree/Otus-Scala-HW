package module5

import zio.*
import zio.stream.*

object ZStreamBasics extends ZIOAppDefault {

  val orders: ZStream[Any, Nothing, Int] =
    ZStream.fromIterable(1 to 20)

  val processedOrders =
    orders
      .filter(_ % 2 == 0)
      .map(_ * 100)
      .take(5)
      .tap(order =>
        Console.printLine(s"Обрабатываем заказ: $order ₽").orDie
      )

  override def run =
    for {
      _ <- Console.printLine("=== START ===")

      result <- processedOrders.runCollect

      _ <- Console.printLine(s"Результат: $result")
    } yield ()
}