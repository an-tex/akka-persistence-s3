object Versions {
  // keep in sync with .travis.yml
  val scala213 = "2.13.1"
  val scala212 = "2.12.11"

  lazy val supportedScalaVersions = List(
    scala213,
    scala212
  )

  val akka = "2.6.4"
  val akkaHttp = "10.1.11"
  val lagom = "1.6.1"
  val alpakka = "2.0.0-RC1"
  val scalaTest = "3.1.1"
}
