ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-dev-mooc-2026-04"
  )

// ScalaTest / Scalactic
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.20"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % Test

// Cats
libraryDependencies += "org.typelevel" %% "cats-core" % "2.13.0"

// Cats Effect
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.7.0"

// ZIO
libraryDependencies += "dev.zio" %% "zio" % "2.1.26"

// ZIO Config
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-config"          % "4.0.8",
  "dev.zio" %% "zio-config-magnolia" % "4.0.8",
  "dev.zio" %% "zio-config-typesafe" % "4.0.8",
  "dev.zio" %% "zio-config-refined"  % "4.0.8"
)

// ZIO Test
libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % "2.1.26" % Test,
  "dev.zio" %% "zio-test-sbt"      % "2.1.26" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.1.26" % Test
)

// FS2
libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "3.12.2",
  "co.fs2" %% "fs2-io"   % "3.12.2"
)

// HTTP4s
val http4sVersion = "0.23.33"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion
)

// Circe
val circeVersion = "0.14.14"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion
)

// Quill / ProtoQuill + ZIO JDBC
val quillVersion = "4.7.3"

libraryDependencies +=
  "io.getquill" %% "quill-jdbc-zio" % quillVersion

// PostgreSQL JDBC Driver
libraryDependencies +=
  "org.postgresql" % "postgresql" % "42.7.7"