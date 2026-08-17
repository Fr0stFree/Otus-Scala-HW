package ru.otus.module4.homework.services

import io.getquill.context.ZioJdbc.QIO
import ru.otus.module4.homework.dao.entity.{Role, RoleCode, User}
import ru.otus.module4.homework.dao.repository.UserRepository
import ru.otus.module4.phoneBook.db
import zio.ZLayer

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


    def listUsersDTO(): QIO[List[UserDTO]] = ???

    def addUserWithRole(user: User, roleCode: RoleCode): QIO[UserDTO] = ???

    def listUsersWithRole(roleCode: RoleCode): QIO[List[UserDTO]] = ???


}
object UserService{

    val layer: ZLayer[UserRepository, Nothing, UserService] = ???
}

case class UserDTO(user: User, roles: Set[Role])