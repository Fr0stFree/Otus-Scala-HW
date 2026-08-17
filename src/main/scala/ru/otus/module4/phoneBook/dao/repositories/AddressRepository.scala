package ru.otus.module4.phoneBook.dao.repositories

import zio.Has
import io.getquill.context.ZioJdbc._
import ru.otus.module4.phoneBook.dao.entities.Address
import zio.{ULayer, ZLayer}
import ru.otus.module4.phoneBook.db

object AddressRepository {
  type AddressRepository = Has[Service]
  
  import db.Ctx._

  trait Service{
      def findBy(id: String): QIO[Option[Address]]
      def insert(phoneRecord: Address): QIO[Unit]
      def update(phoneRecord: Address): QIO[Unit]
      def delete(id: String): QIO[Unit]
  }

  class ServiceImpl extends Service{

      def findBy(id: String): QIO[Option[Address]] = ???
      def insert(address: Address): QIO[Unit] = ???
      def update(address: Address): QIO[Unit] = ???
      
      def delete(id: String): QIO[Unit] = ???
      
  }

  val live: ULayer[AddressRepository] = ZLayer.succeed(new ServiceImpl)
}
