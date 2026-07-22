ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-dev-mooc-2026-04"
  )

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.20"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.13.0"
libraryDependencies += "dev.zio" %% "zio" % "2.1.26"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % "test"
libraryDependencies += "dev.zio" %% "zio-config"          % "4.0.8"
libraryDependencies += "dev.zio" %% "zio-config-magnolia" % "4.0.8"
libraryDependencies += "dev.zio" %% "zio-config-typesafe" % "4.0.8"
libraryDependencies += "dev.zio" %% "zio-config-refined"  % "4.0.8"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-test"          % "2.1.26" % Test,
  "dev.zio" %% "zio-test-sbt"      % "2.1.26" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.1.26" % Test
)
