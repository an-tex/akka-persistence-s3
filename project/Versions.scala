object Versions {
  val akka = "2.5.26"
  val alpakka = "1.1.2"
  val scalaTest = "3.0.8"
  val s3mock = "2.1.16"
  val s3proxy = "1.7.0"

  // keep in sync with .travis.yml
  val scala213 = "2.13.1"
  val scala212 = "2.12.10"
  val scala211 = "2.11.12"

  lazy val supportedScalaVersions = List(scala213, scala212, scala211)
}
