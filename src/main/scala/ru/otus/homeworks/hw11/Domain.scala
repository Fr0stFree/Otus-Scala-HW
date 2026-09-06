package ru.otus.homeworks.hw11

import java.time.Instant
import java.util.UUID

object Domain {
  sealed trait Operation

  object Operation {
    case object Add extends Operation
    case object Subtract extends Operation
    case object Multiply extends Operation
    case object Divide extends Operation
  }

  sealed trait CalculatorEvent

  final case class CalculatorInitialized(
      value: BigDecimal
  ) extends CalculatorEvent

  final case class OperationSucceeded(
      operation: Operation,
      operand: BigDecimal,
      result: BigDecimal
  ) extends CalculatorEvent

  final case class OperationFailed(
      operation: Operation,
      operand: BigDecimal,
      reason: String
  ) extends CalculatorEvent

  final case class EventEnvelope(
      eventId: UUID,
      version: Int,
      aggregateId: UUID,
      createdAt: Instant,
      payload: CalculatorEvent
  )
}
