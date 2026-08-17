ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-dev-mooc-2026-04",

    libraryDependencies ++= Seq(

      // ScalaTest
      "org.scalactic" %% "scalactic" % "3.2.20",
      "org.scalatest" %% "scalatest" % "3.2.20" % Test,

      // Cats
      "org.typelevel" %% "cats-core" % "2.13.0",

      // Cats Effect
      "org.typelevel" %% "cats-effect" % "3.7.0",

      // ZIO
      "dev.zio" %% "zio" % "2.1.26",

      // ZIO Config
      "dev.zio" %% "zio-config" % "4.0.8",
      "dev.zio" %% "zio-config-magnolia" % "4.0.8",
      "dev.zio" %% "zio-config-typesafe" % "4.0.8",
      "dev.zio" %% "zio-config-refined" % "4.0.8",

      // ZIO Test
      "dev.zio" %% "zio-test" % "2.1.26" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.26" % Test,
      "dev.zio" %% "zio-test-magnolia" % "2.1.26" % Test,

      // FS2
      "co.fs2" %% "fs2-core" % "3.12.2",
      "co.fs2" %% "fs2-io" % "3.12.2",

      // HTTP4s
      "org.http4s" %% "http4s-ember-server" % "0.23.33",
      "org.http4s" %% "http4s-ember-client" % "0.23.33",
      "org.http4s" %% "http4s-dsl" % "0.23.33",
      "org.http4s" %% "http4s-circe" % "0.23.33",

      // Circe
      "io.circe" %% "circe-generic" % "0.14.14",
      "io.circe" %% "circe-parser" % "0.14.14",

      // Database
      "org.postgresql" % "postgresql" % "42.7.7",
      "com.zaxxer" % "HikariCP" % "6.3.0",
      "org.liquibase" % "liquibase-core" % "4.33.0"
    )
  )

// Quill / ProtoQuill
libraryDependencies +=
  "io.getquill" %% "quill-jdbc-zio" % "4.6.0.1"

libraryDependencies +=
  "dev.zio" %% "zio-http" % "3.0.1"

libraryDependencies +=
  "dev.zio" %% "zio-interop-cats" % "23.1.0.3"

val testcontainersScalaVersion = "0.43.0"

libraryDependencies ++= Seq(
  "com.dimafeng" %% "testcontainers-scala-core"       % testcontainersScalaVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test
)