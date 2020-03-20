ThisBuild / scalaVersion := Versions.scala213
ThisBuild / organization := "ag.rob"
ThisBuild / organizationName := "Andreas Gabor"

ThisBuild / licenses += ("GPL-3.0", url("https://opensource.org/licenses/GPL-3.0"))

lazy val persistenceS3 = (project in file(".")).aggregate(akkaPersistenceS3, lagomPersistenceS3)

lazy val akkaPersistenceS3 = (project in file("akka-persistence-s3"))
  .settings(
    name := "akka-persistence-s3",
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % Versions.alpakka,
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.typesafe.akka" %% "akka-http-xml" % Versions.akkaHttp,
      "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
      "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test,
      "com.typesafe.akka" %% "akka-serialization-jackson" % Versions.akka % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3"
    ),
    crossScalaVersions := Versions.supportedScalaVersions
  )

lazy val lagomPersistenceS3 = (project in file("lagom-persistence-s3"))
  .settings(
    name := "lagom-persistence-s3",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.lightbend.lagom" %% "lagom-scaladsl-persistence" % Versions.lagom,
    ),
    crossScalaVersions := Versions.supportedScalaVersions
  ).dependsOn(akkaPersistenceS3)

