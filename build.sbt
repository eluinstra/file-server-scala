import Dependencies._

ThisBuild / organization := "dev.luin"
ThisBuild / scalaVersion := "3.1.0"

ThisBuild / scalacOptions ++=
  Seq(
    "-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    // "-Yexplicit-nulls", // experimental (I've seen it cause issues with circe)
    "-Ykind-projector",
    // "-Ysafe-init", // experimental (I've seen it cause issues with circe)
  ) ++ Seq("-rewrite", "-indent") ++ Seq("-source", "future")

lazy val `file-server-scala` =
  project
    .in(file("."))
    .settings(name := "file-server-scala")
    .settings(commonSettings)
    .settings(dependencies)

lazy val commonSettings = commonScalacOptions ++ Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty
)

lazy val commonScalacOptions = Seq(
  Compile / console / scalacOptions --= Seq(
    "-Wunused:_",
    "-Xfatal-warnings",
  ),
  Test / console / scalacOptions :=
    (Compile / console / scalacOptions).value,
)

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio-config" % "1.0.10",
    "dev.zio" %% "zio-config-magnolia" % "1.0.10",
    // "dev.zio" %% "zio-json" % "0.2.0-M3",
    "dev.zio" %% "zio-logging" % "0.5.14",
    "dev.zio" %% "zio-test" % "1.0.13",
    "dev.zio" %% "zio-interop-cats" % "3.2.9.0",
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.20.0-M3",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.20.0-M3",
    // "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "0.20.0-M3",
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "0.20.0-M3",
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "0.20.0-M3",
    "io.circe" %% "circe-core" % "0.14.1"
    // "org.tpolecat" %% "doobie-core" % "0.13.4",
    // "org.tpolecat" %% "doobie-h2" % "0.13.4",
    // "org.tpolecat" %% "doobie-hikari" % "0.13.4",
    // "org.tpolecat" %% "doobie-postgres" % "0.13.4"
  ),
  libraryDependencies ++= Seq(
    org.scalatest.scalatest,
    org.scalatestplus.`scalacheck-1-15`,
  ).map(_ % Test),
)
