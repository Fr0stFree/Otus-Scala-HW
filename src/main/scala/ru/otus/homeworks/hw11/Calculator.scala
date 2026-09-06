package ru.otus.homeworks.hw11

import ru.otus.homeworks.hw11.Domain.*

object Calculator {
  final case class State(value: BigDecimal)

  // Журнал одного калькулятора, в порядке произошедших событий.
  def replay(events: Seq[EventEnvelope]): Option[State] =
    events.foldLeft(Option.empty[State]) { (state, envelope) =>
      evolve(state, envelope.payload)
    }

  def evolve(state: Option[State], event: CalculatorEvent): Option[State] =
    event match {
      case CalculatorInitialized(value) => Some(State(value))

      case OperationSucceeded(operation, operand, result) =>
        state.map(_.copy(result))

      case OperationFailed(_, _, _) => state
    }
}
