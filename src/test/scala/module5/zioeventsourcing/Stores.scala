package module5.zioeventsourcing

import zio.*
import java.time.Instant

final case class StoredEvent(
    aggregateId: AccountId,
    sequence: Long,
    commandId: CommandId,
    event: AccountEvent,
    occurredAt: Instant
) derives CanEqual

final case class Snapshot(
    aggregateId: AccountId,
    version: Long,
    state: AccountState,
    createdAt: Instant
) derives CanEqual

enum StoreError(val message: String) extends AppError:
  case VersionConflict(expected: Long, actual: Long)
      extends StoreError(s"version conflict: expected=$expected, actual=$actual")

  case CorruptedStream(details: String)
      extends StoreError(s"corrupted stream: $details")

enum AppendOutcome derives CanEqual:
  case Appended(events: Chunk[StoredEvent])
  case AlreadyProcessed(currentVersion: Long)

trait EventStore:
  def load(
      aggregateId: AccountId,
      fromExclusive: Long
  ): IO[StoreError, Chunk[StoredEvent]]

  def append(
      aggregateId: AccountId,
      expectedVersion: Long,
      commandId: CommandId,
      events: Chunk[AccountEvent]
  ): IO[StoreError, AppendOutcome]

  def allEvents(aggregateId: AccountId): UIO[Chunk[StoredEvent]]

object EventStore:
  private final case class StreamData(
      events: Vector[StoredEvent],
      processedCommands: Set[CommandId]
  )

  val inMemory: ULayer[EventStore] =
    ZLayer.fromZIO {
      Ref.Synchronized
        .make(Map.empty[AccountId, StreamData])
        .map { ref =>
          new EventStore:
            override def load(
                aggregateId: AccountId,
                fromExclusive: Long
            ): IO[StoreError, Chunk[StoredEvent]] =
              ref.get.map { all =>
                val stream = all.getOrElse(
                  aggregateId,
                  StreamData(Vector.empty, Set.empty)
                )
                Chunk.fromIterable(stream.events.filter(_.sequence > fromExclusive))
              }

            override def append(
                aggregateId: AccountId,
                expectedVersion: Long,
                commandId: CommandId,
                events: Chunk[AccountEvent]
            ): IO[StoreError, AppendOutcome] =
              ref.modifyZIO { all =>
                val stream = all.getOrElse(
                  aggregateId,
                  StreamData(Vector.empty, Set.empty)
                )
                val actualVersion = stream.events.size.toLong

                if stream.processedCommands.contains(commandId) then
                  ZIO.succeed(
                    (AppendOutcome.AlreadyProcessed(actualVersion), all)
                  )
                else if actualVersion != expectedVersion then
                  ZIO.fail(
                    StoreError.VersionConflict(expectedVersion, actualVersion)
                  )
                else if events.isEmpty then
                  val updated = stream.copy(
                    processedCommands = stream.processedCommands + commandId
                  )
                  ZIO.succeed(
                    (
                      AppendOutcome.Appended(Chunk.empty),
                      all.updated(aggregateId, updated)
                    )
                  )
                else
                  Clock.instant.map { now =>
                    val stored = events.zipWithIndex.map { case (event, index) =>
                      StoredEvent(
                        aggregateId = aggregateId,
                        sequence = expectedVersion + index.toLong + 1L,
                        commandId = commandId,
                        event = event,
                        occurredAt = now
                      )
                    }

                    val updated = StreamData(
                      events = stream.events ++ stored,
                      processedCommands = stream.processedCommands + commandId
                    )

                    (
                      AppendOutcome.Appended(stored),
                      all.updated(aggregateId, updated)
                    )
                  }
              }

            override def allEvents(
                aggregateId: AccountId
            ): UIO[Chunk[StoredEvent]] =
              ref.get.map { all =>
                Chunk.fromIterable(
                  all.get(aggregateId).fold(Vector.empty[StoredEvent])(_.events)
                )
              }
        }
    }

trait SnapshotStore:
  def load(aggregateId: AccountId): UIO[Option[Snapshot]]
  def save(snapshot: Snapshot): UIO[Unit]

object SnapshotStore:
  val inMemory: ULayer[SnapshotStore] =
    ZLayer.fromZIO {
      Ref.Synchronized
        .make(Map.empty[AccountId, Snapshot])
        .map { ref =>
          new SnapshotStore:
            override def load(
                aggregateId: AccountId
            ): UIO[Option[Snapshot]] =
              ref.get.map(_.get(aggregateId))

            override def save(snapshot: Snapshot): UIO[Unit] =
              ref.update { all =>
                all.get(snapshot.aggregateId) match
                  case Some(existing) if existing.version >= snapshot.version => all
                  case _ => all.updated(snapshot.aggregateId, snapshot)
              }
        }
    }
