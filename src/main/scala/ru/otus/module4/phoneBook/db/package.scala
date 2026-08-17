package ru.otus.module4.phoneBook

import com.zaxxer.hikari.HikariDataSource
import io.getquill._
import io.getquill.util.LoadConfig
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import ru.otus.module4.phoneBook.configuration.Config
import zio._

package object db {

  type DataSource = javax.sql.DataSource

  object Ctx extends PostgresZioJdbcContext(NamingStrategy(Escape, Literal))

  def hikariDS: HikariDataSource =
    new JdbcContextConfig(LoadConfig("db")).dataSource

  val zioDS: ZLayer[Any, Throwable, DataSource] =
    ZLayer.scoped {
      ZIO.acquireRelease(
        ZIO.attempt(hikariDS).map(ds => ds: DataSource)
      ) {
        case ds: HikariDataSource => ZIO.attempt(ds.close()).orDie
        case _                    => ZIO.unit
      }
    }

  object LiquibaseService {

    trait Service {
      def performMigration: ZIO[Liquibase, Throwable, Unit]
    }

    final class Impl extends Service {
      override def performMigration: ZIO[Liquibase, Throwable, Unit] =
        ZIO.serviceWithZIO[Liquibase] { liquibase =>
          ZIO.attempt(liquibase.update("dev"))
        }
    }

    def performMigration: ZIO[Service & Liquibase, Throwable, Unit] =
      ZIO.serviceWithZIO[Service](_.performMigration)

    val live: ULayer[Service] =
      ZLayer.succeed(new Impl)

    val liquibaseLayer: ZLayer[Config & DataSource, Throwable, Liquibase] =
      ZLayer.scoped {
        for {
          config <- ZIO.service[Config]
          ds     <- ZIO.service[DataSource]
          conn   <- ZIO.acquireRelease(ZIO.attempt(ds.getConnection))(c => ZIO.attempt(c.close()).orDie)
          jdbc    = new JdbcConnection(conn)
          accessor <- ZIO.acquireRelease(
            ZIO.attempt(new ClassLoaderResourceAccessor())
          )(a => ZIO.attempt(a.close()).orDie)
          liquibase <- ZIO.acquireRelease(
            ZIO.attempt(new Liquibase(config.liquibase.changeLog, accessor, jdbc))
          )(_ => ZIO.unit)
        } yield liquibase
      }
  }
}
