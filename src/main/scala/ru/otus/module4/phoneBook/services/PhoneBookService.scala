package ru.otus.module4.phoneBook.services

import ru.otus.module4.phoneBook.dao.entities.{Address, PhoneRecord}
import ru.otus.module4.phoneBook.dao.repositories.{AddressRepository, PhoneRecordRepository}
import ru.otus.module4.phoneBook.db
import ru.otus.module4.phoneBook.db.DataSource
import ru.otus.module4.phoneBook.dto.PhoneRecordDTO
import zio._

object PhoneBookService {

  final case class PhoneRecordNotFound(phone: String)
      extends RuntimeException(s"Phone record not found: $phone")

  type PhoneBookService = Service

  trait Service {
    def find(phone: String): ZIO[DataSource, Throwable, (String, PhoneRecordDTO)]
    def insert(phoneRecord: PhoneRecordDTO): ZIO[DataSource, Throwable, String]
    def update(id: String, addressId: String, phoneRecord: PhoneRecordDTO): ZIO[DataSource, Throwable, Unit]
    def delete(id: String): ZIO[DataSource, Throwable, Unit]
  }

  def find(phone: String): ZIO[Service & DataSource, Throwable, (String, PhoneRecordDTO)] =
    ZIO.serviceWithZIO[Service](_.find(phone))

  def insert(phoneRecord: PhoneRecordDTO): ZIO[Service & DataSource, Throwable, String] =
    ZIO.serviceWithZIO[Service](_.insert(phoneRecord))

  def update(
      id: String,
      addressId: String,
      phoneRecord: PhoneRecordDTO
  ): ZIO[Service & DataSource, Throwable, Unit] =
    ZIO.serviceWithZIO[Service](_.update(id, addressId, phoneRecord))

  def delete(id: String): ZIO[Service & DataSource, Throwable, Unit] =
    ZIO.serviceWithZIO[Service](_.delete(id))

  final class Impl(
      phoneRecordRepository: PhoneRecordRepository.Service,
      addressRepository: AddressRepository.Service
  ) extends Service {

    private val ctx = db.Ctx

    override def find(phone: String): ZIO[DataSource, Throwable, (String, PhoneRecordDTO)] =
      for {
        maybeRecord <- phoneRecordRepository.find(phone)
        record      <- ZIO.fromOption(maybeRecord).orElseFail(PhoneRecordNotFound(phone))
      } yield (record.id, PhoneRecordDTO.from(record))

    override def insert(phoneRecord: PhoneRecordDTO): ZIO[DataSource, Throwable, String] =
      for {
        addressId <- Random.nextUUID.map(_.toString)
        recordId  <- Random.nextUUID.map(_.toString)
        address    = Address(addressId, phoneRecord.zipCode, phoneRecord.address)
        _ <- ctx.transaction {
          for {
            _ <- addressRepository.insert(address)
            _ <- phoneRecordRepository.insert(
              PhoneRecord(recordId, phoneRecord.phone, phoneRecord.fio, address.id)
            )
          } yield ()
        }
      } yield recordId

    override def update(
        id: String,
        addressId: String,
        phoneRecord: PhoneRecordDTO
    ): ZIO[DataSource, Throwable, Unit] =
      ctx.transaction {
        for {
          _ <- addressRepository.update(Address(addressId, phoneRecord.zipCode, phoneRecord.address))
          _ <- phoneRecordRepository.update(
            PhoneRecord(id, phoneRecord.phone, phoneRecord.fio, addressId)
          )
        } yield ()
      }

    override def delete(id: String): ZIO[DataSource, Throwable, Unit] =
      phoneRecordRepository.delete(id)
  }

  val live: ZLayer[PhoneRecordRepository.Service & AddressRepository.Service, Nothing, Service] =
    ZLayer.fromZIO {
      for {
        repo        <- ZIO.service[PhoneRecordRepository.Service]
        addressRepo <- ZIO.service[AddressRepository.Service]
      } yield new Impl(repo, addressRepo)
    }
}
