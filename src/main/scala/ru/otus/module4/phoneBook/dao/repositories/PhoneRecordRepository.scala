package ru.otus.module4.phoneBook.dao.repositories

import io.getquill.context.ZioJdbc._
import zio.{Has, ULayer, ZLayer}
import io.getquill.{EntityQuery, Ord, Quoted}
import ru.otus.module4.phoneBook.dao.entities.{Address, PhoneRecord}
import ru.otus.module4.phoneBook.db

object PhoneRecordRepository {
  val ctx = db.Ctx
  import ctx._

  type PhoneRecordRepository = Has[Service]

  trait Service{
      def find(phone: String): QIO[Option[PhoneRecord]]
      def list(): QIO[List[PhoneRecord]]
      def insert(phoneRecord: PhoneRecord): QIO[Unit]
      def update(phoneRecord: PhoneRecord): QIO[Unit]
      def delete(id: String): QIO[Unit]
  }

  class ServiceImpl extends Service{

    val phoneRecordSchema = quote{
      querySchema[PhoneRecord]("""PhoneRecord""")
    }

    val addressSchema = quote{
      querySchema[Address]("""Address""")
    }

    // SELECT x1."id", x1."phone", x1."fio", x1."addressId" FROM PhoneRecord x1 WHERE x1."phone" = ?
    def find(phone: String): QIO[Option[PhoneRecord]] = 
      ctx.run(phoneRecordSchema.filter(_.phone == lift(phone)).sortBy(_.phone).take(1))
      .map(_.headOption)
    
    // SELECT x."id", x."phone", x."fio", x."addressId" FROM PhoneRecord x
    def list(): QIO[List[PhoneRecord]] = ctx.run(phoneRecordSchema)

    // SELECT p."id", p."phone", p."fio", p."addressId" FROM PhoneRecord p WHERE p."phone" IN (?)
    def list(phones: List[String]): QIO[List[PhoneRecord]] = 
      ctx.run(phoneRecordSchema.filter(p => liftQuery(phones).contains(p.phone)))
    
    // INSERT INTO PhoneRecord ("id","phone","fio","addressId") VALUES (?, ?, ?, ?)
    def insert(phoneRecord: PhoneRecord): QIO[Unit] = 
      ctx.run(phoneRecordSchema.insert(lift(phoneRecord))).unit

    def insert(phoneRecords: List[PhoneRecord]): QIO[Unit] = 
      ctx.run(liftQuery(phoneRecords).foreach{ phr => phoneRecordSchema.insert(phr)}).unit
    
    // UPDATE PhoneRecord SET "id" = ?, "phone" = ?, "fio" = ?, "addressId" = ? WHERE "id" = ?
    def update(phoneRecord: PhoneRecord): QIO[Unit] = 
      ctx.run(phoneRecordSchema.filter(_.id == lift(phoneRecord.id))
      .update(lift(phoneRecord))).unit
    
      // DELETE FROM PhoneRecord WHERE "id" = ?
    def delete(id: String): QIO[Unit] = 
      ctx.run(phoneRecordSchema.filter(_.id == lift(id))
      .delete).unit

    // implicit join

    // SELECT phoneRecord."id", phoneRecord."phone", phoneRecord."fio", 
    // phoneRecord."addressId", address."id", address."zipCode", 
    // address."streetAddress" FROM PhoneRecord phoneRecord, Address address WHERE address."id" = phoneRecord."id"
    
    ctx.run(
      for{
        phoneRecord <- phoneRecordSchema
        address <- addressSchema if (address.id == phoneRecord.id)
      } yield (phoneRecord, address)
    )

    // applicative join
    // SELECT x5."id", x5."phone", x5."fio", x5."addressId", x6."id", x6."zipCode", x6."streetAddress" 
    // FROM PhoneRecord x5 INNER JOIN Address x6 ON x5."addressId" = x6."id"bloop
    ctx.run(phoneRecordSchema.join(addressSchema).on(_.addressId == _.id))

    // flat join
    // SELECT phoneRecord."id", phoneRecord."phone", phoneRecord."fio", phoneRecord."addressId", x7."id", x7."zipCode", x7."streetAddress" 
    // FROM PhoneRecord phoneRecord INNER JOIN Address x7 ON x7."id" = phoneRecord."addressId
    ctx.run(
      for{
        phoneRecord <- phoneRecordSchema
        address <- addressSchema.join(_.id == phoneRecord.addressId)
      } yield (phoneRecord)
    )
    
  }











  

 
  val live: ULayer[PhoneRecordRepository] = ???
}
