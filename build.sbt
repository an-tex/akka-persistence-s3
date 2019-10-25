import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "ag.rob"
ThisBuild / organizationName := "Andreas Gabor"

lazy val root = (project in file("."))
  .settings(
    name := "akka-persistence-s3",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "1.1.2",
      "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
