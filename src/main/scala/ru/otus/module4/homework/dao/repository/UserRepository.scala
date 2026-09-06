package ru.otus.module4.homework.dao.repository

import zio.{ULayer, ZIO, ZLayer}
import io.getquill._
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

  inline def userSchema = quote {
    querySchema[User]("User")
  }

  inline def roleSchema = quote {
    querySchema[Role]("Role")
  }

  inline def userToRoleSchema = quote {
    querySchema[UserToRole]("UserToRole")
  }

  override def findUser(userId: UserId): QIO[Option[User]] = {
    dc.run {
      userSchema.filter(_.id == lift(userId.id)).take(1)
    }.map(_.headOption)
  }

  override def createUser(user: User): QIO[User] = {
    dc.run {
      userSchema.insertValue(lift(user)).returning(user => user)
    }
  }

  override def createUsers(users: List[User]): QIO[List[User]] = {
    dc.run {
      liftQuery(users)
        .foreach(user => {
          userSchema.insertValue(user).returning(user => user)
        })
    }
  }

  override def updateUser(user: User): QIO[Unit] = {
    dc.run {
      userSchema
        .filter(_.id == lift(user.id))
        .updateValue(lift(user))
    }.unit
  }

  override def deleteUser(user: User): QIO[Unit] = {
    dc.run {
      userSchema
        .filter(_.id == lift(user.id))
        .delete
    }.unit
  }

  override def findByLastName(lastName: String): QIO[List[User]] = {
    dc.run {
      userSchema
        .filter(_.lastName == lift(lastName))
    }
  }

  override def list(): QIO[List[User]] = {
    dc.run {
      userSchema
    }
  }

  override def userRoles(userId: UserId): QIO[List[Role]] = {
    dc.run {
      userToRoleSchema
        .filter(_.userId == lift(userId.id))
        .join(roleSchema)
        .on(_.roleId == _.code)
        .map { case (_, role) => role }
    }
  }

  override def insertRoleToUser(roleCode: RoleCode, userId: UserId): QIO[Unit] = {
    dc.run {
      userToRoleSchema.insertValue(
        lift(UserToRole(roleCode.code, userId.id))
      )
    }.unit
  }

  override def listUsersWithRole(roleCode: RoleCode): QIO[List[User]] = {
    dc.run {
      userToRoleSchema
        .filter(_.roleId == lift(roleCode.code))
        .join(userSchema)
        .on(_.userId == _.id)
        .map { case (_, user) => user }
    }
  }

  override def findRoleByCode(roleCode: RoleCode): QIO[Option[Role]] = {
    dc.run {
      roleSchema
        .filter(_.code == lift(roleCode.code))
        .take(1)
    }.map(_.headOption)
  }
}

object UserRepository{

    val layer: ULayer[UserRepository] = ZLayer.succeed(new UserRepositoryImpl)
}