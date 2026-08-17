package ru.otus.module4.homework.dao.repository

import zio.{ULayer, ZIO, ZLayer}
import io.getquill.context.ZioJdbc._
import ru.otus.module4.homework.dao.entity._
import ru.otus.module4.phoneBook.db

import java.sql.SQLException
import javax.sql.DataSource

trait UserRepository{
    def findUser(userId: UserId): QIO[Option[User]]
    def createUser(user: User): QIO[User]
    def createUsers(users: List[User]): QIO[List[User]]
    def updateUser(user: User): QIO[Unit]
    def deleteUser(user: User): QIO[Unit]
    def findByLastName(lastName: String): QIO[List[User]]
    def list(): QIO[List[User]]
    def userRoles(userId: UserId): QIO[List[Role]]
    def insertRoleToUser(roleCode: RoleCode, userId: UserId): QIO[Unit]
    def listUsersWithRole(roleCode: RoleCode): QIO[List[User]]
    def findRoleByCode(roleCode: RoleCode): QIO[Option[Role]]
}


class UserRepositoryImpl extends UserRepository {
    val dc = db.Ctx
    import dc._

    override def findUser(userId: UserId): QIO[Option[User]] = ???

    override def createUser(user: User): QIO[User] = ???

    override def createUsers(users: List[User]): QIO[List[User]] = ???

    override def updateUser(user: User): QIO[Unit] = ???

    override def deleteUser(user: User): QIO[Unit] = ???

    override def findByLastName(lastName: String): QIO[List[User]] = ???

    override def list(): QIO[List[User]] = ???

    override def userRoles(userId: UserId): QIO[List[Role]] = ???

    override def insertRoleToUser(roleCode: RoleCode, userId: UserId): QIO[Unit] = ???

    override def listUsersWithRole(roleCode: RoleCode): QIO[List[User]] = ???

    override def findRoleByCode(roleCode: RoleCode): QIO[Option[Role]] = ???
}

object UserRepository{

    val layer: ULayer[UserRepository] = ZLayer.succeed(new UserRepositoryImpl)
}