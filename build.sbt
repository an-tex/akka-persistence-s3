ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "ag.rob"
ThisBuild / organizationName := "Andreas Gabor"

lazy val root = (project in file("."))
  .settings(
    name := "akka-persistence-s3",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % Versions.alpakka,
      "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test,
      "com.adobe.testing" % "s3mock" % Versions.s3mock % Test,
      "org.gaul" % "s3proxy" % Versions.s3proxy % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
