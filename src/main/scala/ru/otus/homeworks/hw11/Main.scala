package ru.otus.homeworks.hw11

import java.time.Instant
import java.util.UUID
import ru.otus.homeworks.hw11.Domain.*

object Main {
  def main(args: Array[String]): Unit = {
    val calculatorId = UUID.randomUUID()
    val startedAt = Instant.now()
    val payloads = Seq(
      CalculatorInitialized(BigDecimal(0)),
      OperationSucceeded(Operation.Add, BigDecimal(10), BigDecimal(10)),
      OperationSucceeded(Operation.Divide, BigDecimal(2), BigDecimal(5)),
      OperationFailed(Operation.Divide, BigDecimal(0), "ZeroDivisionError"),
      OperationSucceeded(Operation.Multiply, BigDecimal(5), BigDecimal(25))
    )

    val eventLog = payloads
      .map { payload =>
        EventEnvelope(
          eventId = UUID.randomUUID(),
          version = 1,
          aggregateId = calculatorId,
          createdAt = startedAt.plusSeconds(scala.util.Random.nextInt(1000)),
          payload = payload
        )
      }

    println(s"Result: ${Calculator.replay(eventLog)}")
  }
}
