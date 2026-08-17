package ru.otus.module4.phoneBook

import zio._

object Main extends ZIOAppDefault {
  override def run: ZIO[Any, Throwable, Nothing] =
    App.server
}
