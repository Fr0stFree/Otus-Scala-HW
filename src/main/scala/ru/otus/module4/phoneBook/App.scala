package ru.otus.module4.phoneBook

import ru.otus.module4.phoneBook.configuration.Configuration
import ru.otus.module4.phoneBook.dao.repositories.{
  AddressRepository,
  PhoneRecordRepository
}
import ru.otus.module4.phoneBook.db.LiquibaseService
import ru.otus.module4.phoneBook.services.PhoneBookService
import zio.*

object App {

  val server: ZIO[Any, Throwable, Nothing] =
    LiquibaseService.performMigration
      .provide(
        Configuration.live,
        db.zioDS,
        LiquibaseService.liquibaseLayer,
        LiquibaseService.live,
        PhoneRecordRepository.live,
        AddressRepository.live,
        PhoneBookService.live
      ) *>
      ZIO.never
}