package ru.otus.homeworks.hw12

import ru.otus.homeworks.hw12.Domain.*

object Calculator {
  final case class State(value: BigDecimal)

  def evolve(state: Option[State], event: CalculatorEvent): Option[State] = {
    event match {
      case CalculatorInitialized(value)     => Some(State(value))
      case OperationSucceeded(_, _, result) => state.map(_.copy(result))
      case OperationFailed(_, _, _)         => state
    }
  }

  def replay(events: Seq[EventEnvelope]): Option[State] = {
    events.foldLeft(Option.empty[State]) { (state, envelope) =>
      evolve(state, envelope.payload)
    }
  }
}
