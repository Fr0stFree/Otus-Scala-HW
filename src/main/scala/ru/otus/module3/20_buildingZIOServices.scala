package ru.otus.module3

import ru.otus.module2.validation.UserDTO
import ru.otus.module3.emailService.EmailService
import ru.otus.module3.userDAO.UserDAO
import ru.otus.module3.userService.{UserID, UserService}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}


object BuildingZIOServices extends ZIOAppDefault{

  val app: ZIO[UserService, Throwable, Unit] = UserService.notifyUser(UserID(1))

  val appLayer: ZLayer[Any, Nothing, UserService] =
    ZLayer.make[UserService](UserService.live, UserDAO.live, EmailService.live)

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    app.provide(appLayer)
}

