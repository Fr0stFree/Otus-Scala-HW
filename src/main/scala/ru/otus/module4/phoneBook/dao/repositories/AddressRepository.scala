package ru.otus.module4.phoneBook.dao.repositories

import io.getquill._
import io.getquill.context.ZioJdbc._
import ru.otus.module4.phoneBook.dao.entities.Address
import ru.otus.module4.phoneBook.db
import zio.{ULayer, ZLayer}

object AddressRepository {
  import db.Ctx._

  type AddressRepository = Service

  trait Service {
    def findBy(id: String): QIO[Option[Address]]
    def insert(address: Address): QIO[Unit]
    def update(address: Address): QIO[Unit]
    def delete(id: String): QIO[Unit]
  }

  final class ServiceImpl extends Service {
    inline def addressSchema = quote {
      querySchema[Address]("Address")
    }

    override def findBy(id: String): QIO[Option[Address]] =
      db.Ctx.run(addressSchema.filter(_.id == lift(id)).take(1)).map(_.headOption)

    override def insert(address: Address): QIO[Unit] =
      db.Ctx.run(addressSchema.insertValue(lift(address))).unit

    override def update(address: Address): QIO[Unit] =
      db.Ctx
        .run(addressSchema.filter(_.id == lift(address.id)).updateValue(lift(address)))
        .unit

    override def delete(id: String): QIO[Unit] =
      db.Ctx.run(addressSchema.filter(_.id == lift(id)).delete).unit
  }

  val live: ULayer[Service] =
    ZLayer.succeed(new ServiceImpl)
}
