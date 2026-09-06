package module5.zioeventsourcing

import zio.*

final case class LoadedAccount(
    state: AccountState,
    version: Long
) derives CanEqual

trait AccountService:
  def execute(command: AccountCommand): IO[AppError, LoadedAccount]
  def load(accountId: AccountId): IO[StoreError, LoadedAccount]
  def history(accountId: AccountId): UIO[Chunk[StoredEvent]]
  def snapshot(accountId: AccountId): UIO[Option[Snapshot]]

object AccountService:
  final case class Config(
      snapshotEvery: Int,
      maxConflictRetries: Int
  )

  val live: URLayer[EventStore & SnapshotStore, AccountService] =
    ZLayer.fromFunction {
      (eventStore: EventStore, snapshotStore: SnapshotStore) =>
        new Live(
          eventStore,
          snapshotStore,
          Config(snapshotEvery = 5, maxConflictRetries = 20)
        )
    }

  private final class Live(
      eventStore: EventStore,
      snapshotStore: SnapshotStore,
      config: Config
  ) extends AccountService:

    override def execute(
        command: AccountCommand
    ): IO[AppError, LoadedAccount] =
      executeAttempt(command, attempt = 0)

    private def executeAttempt(
        command: AccountCommand,
        attempt: Int
    ): IO[AppError, LoadedAccount] =
      executeOnce(command).catchSome {
        case StoreError.VersionConflict(_, _)
            if attempt < config.maxConflictRetries =>
          ZIO.yieldNow *> executeAttempt(command, attempt + 1)
      }

    private def executeOnce(
        command: AccountCommand
    ): IO[AppError, LoadedAccount] =
      for
        loaded <- load(command.accountId)
        newEvents <- ZIO.fromEither(Account.decide(loaded.state, command))
        outcome <- eventStore.append(
          aggregateId = command.accountId,
          expectedVersion = loaded.version,
          commandId = command.commandId,
          events = newEvents
        )
        result <- outcome match
          case AppendOutcome.AlreadyProcessed(_) =>
            load(command.accountId)

          case AppendOutcome.Appended(stored) =>
            val updatedState = stored.foldLeft(loaded.state) { (state, item) =>
              Account.evolve(state, item.event)
            }
            val updated = LoadedAccount(
              state = updatedState,
              version = loaded.version + stored.length.toLong
            )
            maybeSnapshot(command.accountId, updated).as(updated)
      yield result

    /** Restore state from latest snapshot + only the tail of the event stream. */
    override def load(
        accountId: AccountId
    ): IO[StoreError, LoadedAccount] =
      for
        snap <- snapshotStore.load(accountId)
        baseState = snap.fold(AccountState.empty)(_.state)
        baseVersion = snap.fold(0L)(_.version)
        tail <- eventStore.load(accountId, fromExclusive = baseVersion)
        result <- replay(baseState, baseVersion, tail)
      yield result

    private def replay(
        baseState: AccountState,
        baseVersion: Long,
        events: Chunk[StoredEvent]
    ): IO[StoreError, LoadedAccount] =
      ZIO.fromEither {
        events.foldLeft[Either[StoreError, LoadedAccount]](
          Right(LoadedAccount(baseState, baseVersion))
        ) {
          case (Left(error), _) => Left(error)

          case (Right(acc), item) =>
            val expectedSequence = acc.version + 1L

            if item.sequence != expectedSequence then
              Left(
                StoreError.CorruptedStream(
                  s"expected sequence=$expectedSequence, actual=${item.sequence}"
                )
              )
            else
              Right(
                LoadedAccount(
                  Account.evolve(acc.state, item.event),
                  item.sequence
                )
              )
        }
      }

    private def maybeSnapshot(
        accountId: AccountId,
        loaded: LoadedAccount
    ): UIO[Unit] =
      if loaded.version > 0 && loaded.version % config.snapshotEvery == 0 then
        Clock.instant.flatMap { now =>
          snapshotStore.save(
            Snapshot(
              aggregateId = accountId,
              version = loaded.version,
              state = loaded.state,
              createdAt = now
            )
          )
        }
      else
        ZIO.unit

    override def history(
        accountId: AccountId
    ): UIO[Chunk[StoredEvent]] =
      eventStore.allEvents(accountId)

    override def snapshot(
        accountId: AccountId
    ): UIO[Option[Snapshot]] =
      snapshotStore.load(accountId)
