package ru.otus.module4.phoneBook.services


import ru.otus.module4.phoneBook.dao.entities.{Address, PhoneRecord}
import ru.otus.module4.phoneBook.dao.repositories.{AddressRepository, PhoneRecordRepository}
import ru.otus.module4.phoneBook.db
import ru.otus.module4.phoneBook.db.DataSource
import ru.otus.module4.phoneBook.dto.PhoneRecordDTO
import zio.{Has, RIO, ZIO, ZLayer}
import zio.macros.accessible
import zio.random.Random

@accessible
object PhoneBookService {

     type PhoneBookService = Has[Service]
  
     trait Service{
       def find(phone: String): ZIO[DataSource, Option[Throwable], (String, PhoneRecordDTO)]
       def insert(phoneRecord: PhoneRecordDTO): RIO[DataSource with Random, String]
       def update(id: String, addressId: String, phoneRecord: PhoneRecordDTO): RIO[DataSource, Unit]
       def delete(id: String): RIO[DataSource, Unit]
     }

    class Impl(phoneRecordRepository: PhoneRecordRepository.Service, addressRepository: AddressRepository.Service) extends Service {
       val ctx  = db.Ctx
        def find(phone: String): ZIO[DataSource, Option[Throwable], (String, PhoneRecordDTO)] = for{
          result <- phoneRecordRepository.find(phone).some
        } yield (result.id, PhoneRecordDTO.from(result))

        def insert(phoneRecord: PhoneRecordDTO): RIO[DataSource with Random, String] = for{
          uuid <- zio.random.nextUUID.map(_.toString())
          uuid2 <- zio.random.nextUUID.map(_.toString())
          address = Address(uuid, phoneRecord.zipCode, phoneRecord.address)
          _ <- ctx.transaction(
            for{
             _ <- addressRepository.insert(address)
             _ <- phoneRecordRepository.insert(PhoneRecord(uuid2, phoneRecord.phone, phoneRecord.fio, address.id))
            } yield ()
          )
        } yield uuid
        
        def update(id: String, addressId: String,  phoneRecord: PhoneRecordDTO): RIO[DataSource, Unit] = for{
            _ <- phoneRecordRepository.update(PhoneRecord(id, phoneRecord.phone, phoneRecord.fio, addressId))
        } yield ()
        
        def delete(id: String): RIO[DataSource, Unit] = for{
            _ <- phoneRecordRepository.delete(id)
        } yield ()
        
    }

    val live: ZLayer[PhoneRecordRepository.PhoneRecordRepository with AddressRepository.AddressRepository, Nothing, PhoneBookService.PhoneBookService] = 
      ZLayer.fromServices[PhoneRecordRepository.Service, AddressRepository.Service, PhoneBookService.Service]((repo, addressRepo) => 
        new Impl(repo, addressRepo)
      )

}
