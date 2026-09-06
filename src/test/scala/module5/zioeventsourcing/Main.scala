package module5.zioeventsourcing

import zio.*
import java.util.UUID

/**
 * Run this object directly from IntelliJ.
 *
 * Demonstrates:
 *   1) append-only event stream
 *   2) replay
 *   3) snapshot + tail replay
 *   4) optimistic locking (expectedVersion)
 *   5) retries on concurrent version conflicts
 *   6) command idempotency by commandId
 */
object Main extends ZIOAppDefault:
  import AccountCommand.*

  private def newCommandId: UIO[CommandId] =
    ZIO.succeed(CommandId(UUID.randomUUID()))

  private def printState(
      label: String,
      loaded: LoadedAccount
  ): UIO[Unit] =
    Console
      .printLine(
        f"$label%-28s version=${loaded.version}%2d | " +
          s"status=${loaded.state.status} | " +
          s"balance=${loaded.state.balance} | " +
          s"owner=${loaded.state.owner.getOrElse("-")}"
      )
      .orDie

  private val program: ZIO[AccountService, AppError, Unit] =
    for
      service <- ZIO.service[AccountService]
      accountId = AccountId(UUID.randomUUID())

      openId <- newCommandId
      opened <- service.execute(
        OpenAccount(accountId, openId, "Ada Lovelace")
      )
      _ <- printState("after OpenAccount", opened)

      // Concurrent writes to the same aggregate deliberately create version conflicts.
      // The service reloads and retries with the new expectedVersion.
      depositIds <- ZIO.foreach(1 to 12)(_ => newCommandId)
      _ <- ZIO.foreachParDiscard(depositIds.zipWithIndex) { case (id, index) =>
        service.execute(
          Deposit(
            accountId = accountId,
            commandId = id,
            amount = BigDecimal(10 + index)
          )
        )
      }

      afterDeposits <- service.load(accountId)
      _ <- printState("after 12 deposits", afterDeposits)

      withdrawId <- newCommandId
      afterWithdraw <- service.execute(
        Withdraw(accountId, withdrawId, BigDecimal(50))
      )
      _ <- printState("after Withdraw(50)", afterWithdraw)

      // Same commandId: EventStore sees it as already processed.
      duplicate <- service.execute(
        Withdraw(accountId, withdrawId, BigDecimal(50))
      )
      _ <- printState("same commandId again", duplicate)

      snapshot <- service.snapshot(accountId)
      _ <- Console
        .printLine(
          s"snapshot = ${snapshot.map(s => s"version=${s.version}, balance=${s.state.balance}")}"
        )
        .orDie

      events <- service.history(accountId)
      _ <- Console.printLine("\nEVENT STREAM").orDie
      _ <- ZIO.foreachDiscard(events) { stored =>
        Console
          .printLine(
            f"seq=${stored.sequence}%2d | " +
              s"command=${stored.commandId.value} | " +
              s"event=${stored.event}"
          )
          .orDie
      }

      restored <- service.load(accountId)
      _ <- printState("restored by replay", restored)

      freezeId <- newCommandId
      frozen <- service.execute(
        Freeze(accountId, freezeId, "manual fraud review")
      )
      _ <- printState("after Freeze", frozen)

      illegalWithdrawId <- newCommandId
      illegalWithdraw <- service
        .execute(Withdraw(accountId, illegalWithdrawId, BigDecimal(1)))
        .either
      _ <- Console
        .printLine(s"withdraw from frozen account = $illegalWithdraw")
        .orDie
    yield ()

  override def run: ZIO[Any, Any, Any] =
    program.provide(
      EventStore.inMemory,
      SnapshotStore.inMemory,
      AccountService.live
    )
