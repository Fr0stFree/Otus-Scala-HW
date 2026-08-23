package ru.otus.module3.cats.catsconcurrencyhomework

import cats.effect.{IO, IOApp}
import cats.implicits._
import scala.concurrent.duration._

// Поиграемся с кошельками на файлах и файберами.

// Нужно написать программу где инициализируются три разных кошелька и для каждого из них работает фоновый процесс,
// который регулярно пополняет кошелек на 100 рублей раз в определенный промежуток времени. Промежуток надо сделать разный, чтобы легче было наблюдать разницу.
// Для определенности: первый кошелек пополняем раз в 100ms, второй каждые 500ms и третий каждые 2000ms.
// Помимо этих трёх фоновых процессов (подсказка - это файберы), нужен четвертый, который раз в одну секунду будет выводить балансы всех трех кошельков в консоль.
// Основной процесс программы должен просто ждать ввода пользователя (IO.readline) и завершить программу (включая все фоновые процессы) когда ввод будет получен.
// Итого у нас 5 процессов: 3 фоновых процесса регулярного пополнения кошельков, 1 фоновый процесс регулярного вывода балансов на экран и 1 основной процесс просто ждущий ввода пользователя.

// Можно делать всё на IO, tagless final тут не нужен.

// Подсказка: чтобы сделать бесконечный цикл на IO достаточно сделать рекурсивный вызов через flatMap:
// def loop(): IO[Unit] = IO.println("hello").flatMap(_ => loop())
object WalletFibersApp extends IOApp.Simple {

  private def topUpLoop(wallet: Wallet[IO], amount: BigDecimal, delay: Long): IO[Unit] = {
    for {
      _ <- IO.sleep(delay.millis)
      _ <- wallet.topup(amount)
      _ <- topUpLoop(wallet, amount, delay)
    } yield ()
  }

  private def printBalancesLoop(wallets: Seq[Wallet[IO]]): IO[Unit] = {
    for {
      _ <- IO.sleep(1.second)
      balances <- wallets.traverse(_.balance)
      _ <- IO.println(s"Balances: ${balances.mkString(", ")}")
      _ <- printBalancesLoop(wallets)
    } yield ()
  }

  def run: IO[Unit] =
    for {
      _ <- IO.println("Press any key to stop...")
      wallet1 <- Wallet.fileWallet[IO]("1")
      wallet2 <- Wallet.fileWallet[IO]("2")
      wallet3 <- Wallet.fileWallet[IO]("3")
      
      fiber1 <- topUpLoop(wallet1, 100, 100).start
      fiber2 <- topUpLoop(wallet2, 100, 500).start
      fiber3 <- topUpLoop(wallet3, 100, 2000).start
      fiberPrint <- printBalancesLoop(Seq(wallet1, wallet2, wallet3)).start

      _ <- IO.readLine

      _ <- Seq(fiber1, fiber2, fiber3, fiberPrint).traverse(_.cancel)
    } yield ()

}