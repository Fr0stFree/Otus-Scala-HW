package ru.otus.module4.phoneBook

import ru.otus.module4.phoneBook.api.PhoneBookAPI
import ru.otus.module4.phoneBook.configuration.Configuration
import ru.otus.module4.phoneBook.dao.repositories.{AddressRepository, PhoneRecordRepository}
import ru.otus.module4.phoneBook.db.LiquibaseService
import ru.otus.module4.phoneBook.services.PhoneBookService
import zio._

object App {

  val server: ZIO[Any, Throwable, Nothing] =
    (LiquibaseService.performMigration *> Server.serve(PhoneBookAPI.api))
      .provide(
        Server.default,
        Configuration.live,
        db.zioDS,
        LiquibaseService.liquibaseLayer,
        LiquibaseService.live,
        PhoneRecordRepository.live,
        AddressRepository.live,
        PhoneBookService.live
      )
}
