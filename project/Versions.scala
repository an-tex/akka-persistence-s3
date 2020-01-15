object Versions {
  val akkaPersistenceS3 = "0.5.2"
  val lagomPersistenceS3 = "0.4.2"

  // keep in sync with .travis.yml
  val scala213 = "2.13.1"
  val scala212 = "2.12.10"

  lazy val supportedScalaVersions = List(
    scala213,
    scala212
  )

  val akka = "2.6.1"
  val akkaHttp = "10.1.11"
  val lagom = "1.6.0"
  val alpakka = "2.0.0-M2"
  val scalaTest = "3.0.8"
}
