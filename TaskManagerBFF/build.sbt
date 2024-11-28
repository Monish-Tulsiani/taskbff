ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.15"

lazy val root = (project in file("."))
  .settings(
    name := "BE"
  )

val Http4sVersion = "1.0.0-M21"
val CirceVersion = "0.14.9"
libraryDependencies ++= Seq(
  "org.typelevel" %% "log4cats-slf4j" % "2.5.0",
  "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s"      %% "http4s-circe"        % Http4sVersion,
  "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
  "io.circe"        %% "circe-generic"       % CirceVersion,
  "io.circe"        %% "circe-core"          % CirceVersion,
  "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
  "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC1",
  "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
  "org.postgresql" % "postgresql" % "42.5.0",
  "com.auth0" % "java-jwt" % "4.4.0"
)