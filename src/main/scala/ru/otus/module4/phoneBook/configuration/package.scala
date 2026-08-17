package ru.otus.module4.phoneBook

import com.typesafe.config.ConfigFactory
import zio._

package object configuration {

  final case class Config(api: Api, liquibase: LiquibaseConfig)
  final case class LiquibaseConfig(changeLog: String)
  final case class Api(host: String, port: Int)
  final case class DbConfig(driver: String, url: String, user: String, password: String)

  type Configuration = Config

  object Configuration {
    val live: TaskLayer[Config] =
      ZLayer.fromZIO {
        ZIO.attempt {
          val c = ConfigFactory.load()
          Config(
            api = Api(
              host = if (c.hasPath("api.host")) c.getString("api.host") else "0.0.0.0",
              port = if (c.hasPath("api.port")) c.getInt("api.port") else 8080
            ),
            liquibase = LiquibaseConfig(
              changeLog =
                if (c.hasPath("liquibase.changeLog")) c.getString("liquibase.changeLog")
                else "db/changelog.xml"
            )
          )
        }
      }
  }
}
