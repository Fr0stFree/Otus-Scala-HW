package ru.otus.module4.phoneBook

import io.circe._
import io.circe.generic.semiauto._
import ru.otus.module4.phoneBook.dao.entities.PhoneRecord

import scala.util.{Failure, Success, Try}

package object dto {

    case class PhoneRecordDTO(phone: String, fio: String, zipCode: String, address: String)

    object PhoneRecordDTO{
        implicit val decoder: Decoder[PhoneRecordDTO] = deriveDecoder
        implicit val encoder: Encoder[PhoneRecordDTO] = deriveEncoder

        def from(phoneRecord: PhoneRecord): PhoneRecordDTO = PhoneRecordDTO(
            phoneRecord.phone,
            phoneRecord.fio,
            "",
            ""
        ) 
    }

    case class RecordId(id: Int)

    object IdVar{
        def unapply(str: String): Option[RecordId] = {
            Try(str.toInt) match {
                case Failure(_) => None
                case Success(value) if value < 0 => None
            }
        }
    }
}
