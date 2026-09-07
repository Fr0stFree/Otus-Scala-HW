package ru.otus.homeworks.hw13

import ru.otus.homeworks.hw12.Calculator.State
import ru.otus.homeworks.hw12.Domain.*

sealed trait Command
object Command {
  final case class Initialize(value: BigDecimal) extends Command
  final case class Calculate(operation: Operation, operand: BigDecimal)
      extends Command
  case object Reset extends Command
}

sealed trait DomainError
object DomainError {
  case object AlreadyInitialized extends DomainError
  case object NotInitialized extends DomainError
  case object DivisionByZero extends DomainError
}

object CommandHandler {
  def handle(
      state: Option[State],
      command: Command
  ): Either[DomainError, Seq[CalculatorEvent]] = {
    command match {
      case cmd: Command.Initialize => handleInitialize(state, cmd)
      case cmd: Command.Calculate  => handleCommand(state, cmd)
      case cmd: Command.Reset.type => handleReset(state, cmd)
    }
  }

  def handleInitialize(
      state: Option[State],
      command: Command.Initialize
  ): Either[DomainError, Seq[CalculatorEvent]] = {
    if (state.isDefined) Left(DomainError.AlreadyInitialized)
    else Right(Seq(CalculatorInitialized(command.value)))
  }

  def handleReset(
      state: Option[State],
      command: Command.Reset.type
  ): Either[DomainError, Seq[CalculatorEvent]] = {
    state
      .toRight(DomainError.NotInitialized)
      .map { current =>
        if (current.value == 0) Nil
        else Seq(CalculatorInitialized(BigDecimal(0)))
      }
  }

  def handleCommand(
      state: Option[State],
      command: Command.Calculate
  ): Either[DomainError, Seq[CalculatorEvent]] = {

    def validateOperation(operation: Operation) = {
      operation match {
        case Operation.Divide if command.operand == 0 =>
          Left(DomainError.DivisionByZero)
        case _ => Right(operation)
      }
    }

    for {
      current <- state.toRight(DomainError.NotInitialized)
      operation <- validateOperation(command.operation)
      result = operation match {
        case Operation.Add      => current.value + command.operand
        case Operation.Subtract => current.value - command.operand
        case Operation.Multiply => current.value * command.operand
        case Operation.Divide   => current.value / command.operand
      }
    } yield Seq(OperationSucceeded(operation, command.operand, result))
  }
}
