package module5

import zio.*
import zio.stream.*

object ZStreamEffects extends ZIOAppDefault {

  case class Order(id: Int)

  def loadFromDatabase(id: Int): UIO[Order] =
    for {
      _ <- Console.printLine(s"DB      -> loading order $id").orDie
      _ <- ZIO.sleep(500.millis)
    } yield Order(id)

  def sendNotification(order: Order): UIO[Unit] =
    Console
      .printLine(s"NOTIFY  -> order ${order.id}")
      .orDie

  val orderIds =
    ZStream.fromIterable(1 to 5)

  override def run =
    orderIds
      .mapZIOPar(5)(loadFromDatabase)
      .tap(sendNotification)
      .runDrain
}