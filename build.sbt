ThisBuild / scalaVersion := Versions.scala212
ThisBuild / organization := "ag.rob"
ThisBuild / organizationName := "Andreas Gabor"

ThisBuild / licenses += ("GPL-3.0", url("https://opensource.org/licenses/GPL-3.0"))

publish / skip := isSnapshot.value

lazy val root = (project in file(".")).aggregate(akkaPersistenceS3, lagomPersistenceS3)

lazy val akkaPersistenceS3 = (project in file("akka-persistence-s3"))
  .settings(
    name := "akka-persistence-s3",
    version := Versions.akkaPersistenceS3,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % Versions.alpakka,
      "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test,
      "org.gaul" % "s3proxy" % Versions.s3proxy % Test
    ),
    crossScalaVersions := Versions.supportedScalaVersions
  )

lazy val lagomPersistenceS3 = (project in file("lagom-persistence-s3"))
  .settings(
    name := "lagom-persistence-s3",
    version := Versions.lagomPersistenceS3,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.lightbend.lagom" %% "lagom-scaladsl-persistence" % Versions.lagom,
    ),
    crossScalaVersions := Versions.supportedScalaVersions
  ).dependsOn(akkaPersistenceS3)
