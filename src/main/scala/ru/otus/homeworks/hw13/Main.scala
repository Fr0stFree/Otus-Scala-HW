package ru.otus.homeworks.hw13

import ru.otus.homeworks.hw12.Calculator
import ru.otus.homeworks.hw12.Domain.*

object Main {
  def main(args: Array[String]): Unit = {
    val commands = Seq(
      Command.Calculate(Operation.Add, BigDecimal(1)),
      Command.Initialize(BigDecimal(0)),
      Command.Initialize(BigDecimal(10)),
      Command.Calculate(Operation.Add, BigDecimal(10)),
      Command.Calculate(Operation.Divide, BigDecimal(0)),
      Command.Calculate(Operation.Divide, BigDecimal(2)),
      Command.Reset,
      Command.Reset
    )

    val history = commands.foldLeft(Seq.empty[CalculatorEvent]) {
      (events, command) =>
        val state =
          events.foldLeft(Option.empty[Calculator.State])(Calculator.evolve)
        val result = CommandHandler.handle(state, command)
        result.fold(_ => events, generated => events ++ generated)
    }
    val state =
      history.foldLeft(Option.empty[Calculator.State])(Calculator.evolve)

    println(s"History: $history")
    println(s"Replayed state: $state")
  }
}
