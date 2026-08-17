package ru.otus.module4

import com.dimafeng.testcontainers.PostgreSQLContainer
import zio.{Scope, ULayer, ZIO, ZLayer}


object TestContainer {


  def postgres(): ULayer[PostgreSQLContainer] =
    ZLayer.scoped(ZIO.acquireRelease {
      ZIO.attemptBlocking {
        val container = new PostgreSQLContainer()
        container.start()
        container
      }.orDie
    }(container => ZIO.attemptBlocking(container.stop()).orDie))
}