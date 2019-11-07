object Versions {
  val akkaPersistenceS3 = "0.3"
  val lagomPersistenceS3 = "0.1"

  // keep in sync with .travis.yml
  val scala213 = "2.13.1"
  val scala212 = "2.12.10"

  lazy val supportedScalaVersions = List(
    //scala213,
    scala212
  )

  val akka = "2.5.26"
  val lagom = "1.5.4"
  val alpakka = "1.1.2"
  val scalaTest = "3.0.8"
  val s3mock = "2.1.16"
  val s3proxy = "1.7.0"
}
