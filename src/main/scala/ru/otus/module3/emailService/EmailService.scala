package ru.otus.module3.emailService

import zio.{Console, UIO, ULayer, ZLayer}


/**
 * Реализовать Сервис с одним методом sendEmail,
 * который будет принимать Email и отправлять его
 */

trait EmailService {
  def sendEmail(email: Email): UIO[Unit]
}

object EmailService {
  val live: ULayer[EmailService] = ZLayer.succeed(new EmailService {
    override def sendEmail(email: Email): UIO[Unit] =
      Console.printLine(email.toString).orDie
  })
}





