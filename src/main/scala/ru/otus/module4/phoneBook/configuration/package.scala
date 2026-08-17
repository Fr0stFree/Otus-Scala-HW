package ru.otus.module4.phoneBook

import zio._
import zio.config.ReadError
import zio.config.typesafe.TypesafeConfig

package object configuration {

  case class Config(api: Api, liquibase: LiquibaseConfig)
  
  case class LiquibaseConfig(changeLog: String)
  case class Api(host: String, port: Int)
  case class DbConfig(driver: String, url: String, user: String, password: String)
  

  import zio.config.magnolia.DeriveConfigDescriptor.descriptor

  val configDescriptor = descriptor[Config]

  type Configuration = zio.Has[Config]
  
  object Configuration{
    val live: Layer[ReadError[String], Configuration] = TypesafeConfig.fromDefaultLoader(configDescriptor)
  }
}
