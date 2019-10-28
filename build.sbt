ThisBuild / scalaVersion := Versions.scala213
ThisBuild / version := "0.3"
ThisBuild / organization := "ag.rob"
ThisBuild / organizationName := "Andreas Gabor"

licenses += ("GPL-3.0", url("https://opensource.org/licenses/GPL-3.0"))

publish / skip := isSnapshot.value

lazy val root = (project in file("."))
  .settings(
    name := "akka-persistence-s3",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % Versions.alpakka,
      "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test,
      "org.gaul" % "s3proxy" % Versions.s3proxy % Test
    ),
    crossScalaVersions := Versions.supportedScalaVersions
  )
