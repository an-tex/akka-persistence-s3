object Versions {
  val akkaPersistenceS3 = "0.4-SNAPSHOT"
  val lagomPersistenceS3 = "0.2-SNAPSHOT"

  // keep in sync with .travis.yml
  val scala213 = "2.13.1"
  val scala212 = "2.12.10"

  lazy val supportedScalaVersions = List(
    //scala213,
    scala212
  )

  val akka = "2.5.26"
  val lagom = "1.5.4"
  val alpakka = "2.0.0-M1+2-3a7f0aef+20191119-1136"
  val scalaTest = "3.0.8"
  val s3mock = "2.1.16"
  val s3proxy = "1.7.0"
}
