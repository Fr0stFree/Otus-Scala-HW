import java.time.Instant
import java.util.UUID

// ------------------------------------------------------------
// 1. Domain event: факт, который уже произошёл
// ------------------------------------------------------------

sealed trait DomainEvent

final case class OrderPlaced(
                              orderId: String,
                              customerId: String,
                              total: BigDecimal
                            ) extends DomainEvent

// ------------------------------------------------------------
// 2. Envelope: техническая информация вокруг доменного события
// ------------------------------------------------------------

final case class EventEnvelope[E <: DomainEvent](
                                                  eventId: UUID,
                                                  aggregateId: String,
                                                  eventType: String,
                                                  version: Int,
                                                  occurredAt: Instant,
                                                  correlationId: Option[UUID],
                                                  causationId: Option[UUID],
                                                  producer: String,
                                                  payload: E
                                                )

// ------------------------------------------------------------
// 3. Event log: append-only история событий
// ------------------------------------------------------------

final class InMemoryEventLog:
  private var events: Vector[EventEnvelope[? <: DomainEvent]] =
    Vector.empty

  def append[E <: DomainEvent](event: EventEnvelope[E]): Unit =
    events = events :+ event

  def all: Vector[EventEnvelope[? <: DomainEvent]] =
    events

  def byAggregateId(
                     aggregateId: String
                   ): Vector[EventEnvelope[? <: DomainEvent]] =
    events.filter(_.aggregateId == aggregateId)

// ------------------------------------------------------------
// 4. Aggregate state
//
// Это НЕ CQRS read model.
// Это состояние aggregate, которое можно восстановить из событий.
// ------------------------------------------------------------

final case class OrderState(
                             orderId: String,
                             customerId: String,
                             total: BigDecimal
                           )

object Order:

  def evolve(
              state: Option[OrderState],
              event: DomainEvent
            ): Option[OrderState] =
    event match
      case e: OrderPlaced =>
        Some(
          OrderState(
            orderId = e.orderId,
            customerId = e.customerId,
            total = e.total
          )
        )

  def replay(
              events: Seq[EventEnvelope[? <: DomainEvent]]
            ): Option[OrderState] =
    events.foldLeft(Option.empty[OrderState]) {
      case (state, envelope) =>
        evolve(state, envelope.payload)
    }

// ------------------------------------------------------------
// 5. Demo
// ------------------------------------------------------------

@main def runEventExample(): Unit =

  val eventLog = new InMemoryEventLog

  val correlationId =
    UUID.randomUUID()

  val orderPlaced =
    OrderPlaced(
      orderId = "order-123",
      customerId = "customer-42",
      total = BigDecimal("3250.00")
    )

  val envelope =
    EventEnvelope(
      eventId = UUID.randomUUID(),
      aggregateId = orderPlaced.orderId,
      eventType = "OrderPlaced",
      version = 1,
      occurredAt = Instant.now(),
      correlationId = Some(correlationId),
      causationId = None,
      producer = "order-service",
      payload = orderPlaced
    )

  // событие только добавляется в log
  eventLog.append(envelope)

  println("=== Event log ===")

  eventLog.all.zipWithIndex.foreach {
    case (event, offset) =>
      println(
        s"offset=$offset, " +
          s"type=${event.eventType}, " +
          s"aggregateId=${event.aggregateId}, " +
          s"payload=${event.payload}"
      )
  }

  println()
  println("=== Replay aggregate state ===")

  val orderEvents =
    eventLog.byAggregateId("order-123")

  val state =
    Order.replay(orderEvents)

  println(state)

  println()
  println("=== Important idea ===")
  println("Domain event = факт")
  println("Event log = история")
  println("Replay = восстановление aggregate state")
  println("CQRS projection/read model появится позже")
