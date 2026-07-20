package ru.otus.module3.userDAO

import ru.otus.module3.emailService.EmailAddress
import ru.otus.module3.userService.{User, UserID}
import zio.{Task, ULayer, ZIO, ZLayer}

/**
 * Реализовать сервис с двумя методами
 *  1. list - список всех пользователей
 *  2. findBy - поиск по User ID
 */


trait UserDAO {
  def list(): Task[List[User]]
  def findBy(userID: UserID): Task[Option[User]]
}

object UserDAO {
  val live = ZLayer.succeed(
    new UserDAO {

      val users = List(
        User(UserID(1), EmailAddress("email1@gmail.com")),
        User(UserID(2), EmailAddress("email2@gmail.com")),
        User(UserID(3), EmailAddress("email3@gmail.com")),
      )
      override def list(): Task[List[User]] = ZIO.attempt(users)

      override def findBy(userID: UserID): Task[Option[User]] =
        list().map(_.find(_.id == userID))
    }
  )
}