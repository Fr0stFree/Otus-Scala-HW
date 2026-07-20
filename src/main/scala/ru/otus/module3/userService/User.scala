package ru.otus.module3.userService

import ru.otus.module3.emailService.EmailAddress


case class User(id: UserID, email: EmailAddress)

case class UserID(id: Int) extends AnyVal