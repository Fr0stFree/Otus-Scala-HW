package ru.otus.module4.phoneBook

import zio._


object Main extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    App.server.exitCode
}
