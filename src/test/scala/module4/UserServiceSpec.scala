package ru.otus.module4

import ru.otus.module4.homework.dao.entity.{Role, RoleCode, User}
import ru.otus.module4.homework.dao.repository.UserRepository
import ru.otus.module4.homework.services.UserService
import zio.ZIO
import zio.test.Assertion._
import zio.test._

import java.util.UUID


object UserServiceSpec extends ZIOSpecDefault {

  import MigrationAspects._

  private val dc = DBTransactor.Ctx


  private val users = List(
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString, scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120))
  )
  private val usersGen = Gen.fromIterable(users)

  private val Manager = RoleCode("manager")

  def spec = suite("UserServiceSpec")(
    test("add user with role")(
      for {
        userService <- ZIO.service[UserService]
        _ <- userService.addUserWithRole(users.head, Manager)
        result <- userService.listUsersDTO()
      } yield assert(result.length)(equalTo(1)) &&
        assert(result.head.user)(equalTo(users.head)) && assert(result.head.roles)(equalTo(Set(Role(Manager.code, "Manager"))))
    ) @@ migrate(),
    test("list user with role Manager should return empty List")(
      for {
        userRepo <- ZIO.service[UserRepository]
        userService <- ZIO.service[UserService]
        _ <- userRepo.createUsers(users)
        result <- userService.listUsersWithRole(Manager)
      } yield assert(result)(isEmpty)
    ) @@ migrate(),
    test("list user with role Manager should return one Entry")(
      for {
        userRepo <- ZIO.service[UserRepository]
        userService <- ZIO.service[UserService]
        _ <- userRepo.createUsers(users.tail)
        _ <- userService.addUserWithRole(users.head, Manager)
        result <- userService.listUsersWithRole(Manager)
      } yield assert(result.length)(equalTo(1)) && assert(result.head.user)(equalTo(users.head)) &&
        assert(result.head.roles)(equalTo(Set(Role(Manager.code, "Manager"))))
    ) @@ migrate()
  ).provideShared(
    TestContainer.postgres(),
    DBTransactor.test,
    LiquibaseService.liquibaseLayer,
    UserRepository.layer,
    UserService.layer
  )
}