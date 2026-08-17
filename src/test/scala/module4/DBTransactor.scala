package ru.otus.module4

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.config.Config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill._
import zio.{ZIO, ZLayer}

object DBTransactor {

  type DataSource = javax.sql.DataSource

  object Ctx extends PostgresZioJdbcContext(NamingStrategy(Escape, Literal))

  def hikariDS(config: Config): HikariDataSource = JdbcContextConfig(config).dataSource

  def test: ZLayer[PostgreSQLContainer, Throwable, HikariDataSource] = ZLayer(
    for {
      pg <- ZIO.service[PostgreSQLContainer]
      config <- ZIO.attempt {
        val hc = new HikariConfig()
        hc.setUsername(pg.username)
        hc.setPassword(pg.password)
        hc.setJdbcUrl(pg.jdbcUrl)
        hc.setDriverClassName(pg.driverClassName)
        hc
      }
      ds <- ZIO.attempt(new HikariDataSource(config))
    } yield ds
  )

}