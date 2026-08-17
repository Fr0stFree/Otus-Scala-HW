package ru.otus.module4.phoneBook.dao.repositories

import io.getquill._
import io.getquill.context.ZioJdbc._
import ru.otus.module4.phoneBook.dao.entities.PhoneRecord
import ru.otus.module4.phoneBook.db
import zio.{ULayer, ZLayer}

object PhoneRecordRepository {
  import db.Ctx._

  type PhoneRecordRepository = Service

  trait Service {
    def find(phone: String): QIO[Option[PhoneRecord]]
    def list(): QIO[List[PhoneRecord]]
    def insert(phoneRecord: PhoneRecord): QIO[Unit]
    def update(phoneRecord: PhoneRecord): QIO[Unit]
    def delete(id: String): QIO[Unit]
  }

  final class ServiceImpl extends Service {
    inline def phoneRecordSchema = quote {
      querySchema[PhoneRecord]("PhoneRecord")
    }

    override def find(phone: String): QIO[Option[PhoneRecord]] =
      db.Ctx
        .run(phoneRecordSchema.filter(_.phone == lift(phone)).sortBy(_.phone).take(1))
        .map(_.headOption)

    override def list(): QIO[List[PhoneRecord]] =
      db.Ctx.run(phoneRecordSchema)

    override def insert(phoneRecord: PhoneRecord): QIO[Unit] =
      db.Ctx.run(phoneRecordSchema.insertValue(lift(phoneRecord))).unit

    override def update(phoneRecord: PhoneRecord): QIO[Unit] =
      db.Ctx
        .run(
          phoneRecordSchema
            .filter(_.id == lift(phoneRecord.id))
            .updateValue(lift(phoneRecord))
        )
        .unit

    override def delete(id: String): QIO[Unit] =
      db.Ctx.run(phoneRecordSchema.filter(_.id == lift(id)).delete).unit
  }

  val live: ULayer[Service] =
    ZLayer.succeed(new ServiceImpl)
}
