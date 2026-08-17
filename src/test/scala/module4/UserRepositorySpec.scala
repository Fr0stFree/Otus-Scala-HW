package ru.otus.module4

import ru.otus.module4.homework.dao.entity.{User, UserId}
import ru.otus.module4.homework.dao.repository.UserRepository
import zio.test.Assertion._
import zio.test._
import zio.{Random, ZIO}

import java.util.UUID


object UserRepositorySpec extends ZIOSpecDefault {

  import MigrationAspects._

  val dc = DBTransactor.Ctx


  val genName: Gen[Random with Sized, String] = Gen.asciiString
  val genAge: Gen[Random, Int] = Gen.int(18, 120)
  val genUuid: Gen[Random, UUID] = Gen.uuid

  val genUser = for {
    uuid <- genUuid
    firstName <- genName
    lastName <- genName
    age <- genAge
  } yield User(uuid.toString(), firstName, lastName, age)


  val users = List(
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120)),
    User(UUID.randomUUID().toString(), scala.util.Random.nextString(15), scala.util.Random.nextString(30), scala.util.Random.nextInt(120))
  )
  val usersGen = Gen.fromIterable(users)


  def spec = suite("UserRepositorySpec")(
    test("метод list возвращает пустую коллекцию, на пустой базе")(
      for {
        userRepo <- ZIO.service[UserRepository]
        result <- userRepo.list()
      } yield assert(1)(equalTo(1)) &&
        assert(result.isEmpty)(equalTo(true))
    ) @@ migrate(),
    test("методы create а затем findBy по созданному пользователю")(
      checkAll(usersGen) { user =>
        for {
          userRepo <- ZIO.service[UserRepository]
          user <- userRepo.createUser(user)
          result <- userRepo.findUser(user.typedId)
            .some.mapError(_ => new Exception("fetch failed"))
        } yield assert(user.id)(equalTo(result.id)) &&
          assert(result.firstName)(equalTo(user.firstName))
      }

    ) @@ migrate(),
    test("метод findBy по случайному id")(
      checkAll(usersGen, Gen.uuid) { (user, id) =>
        for {
          userRepo <- ZIO.service[UserRepository]
          user <- userRepo.createUser(user)
          result <- userRepo.findUser(UserId(id.toString()))
        } yield assert(result)(isNone)
      }

    ) @@ migrate(),
    test("метод update должен обновлять только целевого пользователя")(
      for {
        userRepo <- ZIO.service[UserRepository]
        users <- userRepo.createUsers(users)
        user = users.head
        newFirstName = "Petr"
        _ <- userRepo.updateUser(user.copy(firstName = newFirstName))
        updated <- userRepo.findUser(user.typedId).some.mapError(_ => new Exception("fetch failed"))
        all <- userRepo.list()

      } yield assert(updated.firstName)(equalTo(newFirstName)) &&
        assert(all.filter(_.id != user.id).toSet)(equalTo(users.filter(_.id != user.id).toSet))

    ) @@ migrate(),
    test("метод delete должен удалять только целевого пользователя")(
      for {
        userRepo <- ZIO.service[UserRepository]
        users <- userRepo.createUsers(users)
        user = users.last
        _ <- userRepo.deleteUser(user)
        all <- userRepo.list()

      } yield assert(all.length)(equalTo(9)) &&
        assert(all.toSet)(equalTo(users.filter(_.id != user.id).toSet))

    ) @@ migrate(),
    test("метод findByLastName должен находить пользователя")(
      for {
        userRepo <- ZIO.service[UserRepository]
        users <- userRepo.createUsers(users)
        user = users(5)
        result <- userRepo.findByLastName(user.lastName)
      } yield assert(result.length)(equalTo(1)) &&
        assert(result.head.lastName)(equalTo(user.lastName))

    ) @@ migrate(),

  ).provideShared(
    TestContainer.postgres(),
    DBTransactor.test,
    LiquibaseService.liquibaseLayer,
    UserRepository.layer
  )
}