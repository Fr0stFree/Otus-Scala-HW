package ru.otus.module4.homework.services

import io.getquill.context.ZioJdbc._
import ru.otus.module4.homework.dao.entity.{Role, RoleCode, User}
import ru.otus.module4.homework.dao.repository.UserRepository
import ru.otus.module4.phoneBook.db
import zio.{ZLayer, ZIO}

import java.sql.SQLException

trait UserService{
    def listUsers(): QIO[List[User]]
    def listUsersDTO(): QIO[List[UserDTO]]
    def addUserWithRole(user: User, roleCode: RoleCode): QIO[UserDTO]
    def listUsersWithRole(roleCode: RoleCode): QIO[List[UserDTO]]
}
class Impl(userRepo: UserRepository) extends UserService {
    val dc = db.Ctx

    def listUsers(): QIO[List[User]] =
        userRepo.list()


    def listUsersDTO(): QIO[List[UserDTO]] =
      for {
        users <- userRepo.list()
        result <- ZIO.foreach(users) { user =>
          userRepo
            .userRoles(user.typedId)
            .map(roles => UserDTO(user, roles.toSet))
          }
      } yield result

    def addUserWithRole(user: User, roleCode: RoleCode): QIO[UserDTO] =
      dc.transaction {
        for {
          role <- userRepo.findRoleByCode(roleCode).flatMap {
            case Some(role) => ZIO.succeed(role)
            case None => ZIO.fail(new SQLException(s"Role '${roleCode.code}' not found"))
          }
          createdUser <- userRepo.createUser(user)
          _ <- userRepo.insertRoleToUser(roleCode, createdUser.typedId)
        } yield UserDTO(createdUser, Set(role))
      }.mapError {
        case error => new SQLException("Failed to add user with role", error) 
      }

    def listUsersWithRole(roleCode: RoleCode): QIO[List[UserDTO]] =
      for {
        users <- userRepo.listUsersWithRole(roleCode)
        result <- ZIO.foreach(users) { user =>
          userRepo
            .userRoles(user.typedId)
            .map(roles => UserDTO(user, roles.toSet))
        }
      } yield result
}
object UserService{

    val layer: ZLayer[UserRepository, Nothing, UserService] = ZLayer.fromFunction(new Impl(_))
}

case class UserDTO(user: User, roles: Set[Role])