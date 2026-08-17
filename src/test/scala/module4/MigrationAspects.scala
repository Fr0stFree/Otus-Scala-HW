package ru.otus.module4

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.{ClassLoaderResourceAccessor, CompositeResourceAccessor, FileSystemResourceAccessor}
import liquibase.servicelocator.LiquibaseService
import ru.otus.module4.DBTransactor.DataSource
import zio.test.TestAspect._
import zio.{RIO, Scope, ZIO, ZLayer}

object MigrationAspects {

  def migrate() = {
    before(LiquibaseService.performMigration.orDie)
  }

}

object LiquibaseService {
  def performMigration: RIO[Liquibase, Unit] = ZIO.serviceWith[Liquibase](_.update("dev"))

  def mkLiquibase(): ZIO[Any with Scope with DataSource, Throwable, Liquibase] = for {
    ds <- ZIO.service[DataSource]
    fileAccessor <- ZIO.attempt(new FileSystemResourceAccessor())
    classLoader <- ZIO.attempt(classOf[LiquibaseService].getClassLoader)
    classLoaderAccessor <- ZIO.attempt(new ClassLoaderResourceAccessor(classLoader))
    fileOpener <- ZIO.attempt(new CompositeResourceAccessor(fileAccessor, classLoaderAccessor))
    jdbcConn <- ZIO.acquireRelease(ZIO.attempt(new JdbcConnection(ds.getConnection())))(conn => ZIO.succeed(conn.close()))
    liqui <- ZIO.attempt(new Liquibase("src/test/resources/liquibase/main.xml", fileOpener, jdbcConn))
  } yield liqui


  val liquibaseLayer: ZLayer[DataSource, Throwable, Liquibase] = ZLayer.scoped(mkLiquibase())

}