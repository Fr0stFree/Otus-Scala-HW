package ru.otus.module3.zio_homework

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object ZioHomeWorkApp extends ZIOAppDefault{
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???

}
