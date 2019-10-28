[![Build Status](https://travis-ci.com/an-tex/akka-persistence-s3.svg?branch=master)](https://travis-ci.com/an-tex/akka-persistence-s3)

[ ![Download](https://api.bintray.com/packages/antex/maven/akka-persistence-s3/images/download.svg) ](https://bintray.com/antex/maven/akka-persistence-s3/_latestVersion)

Akka Persistence Backend Plugin using S3. It's not production ready yet but passes the [Plugin TCK](https://doc.akka.io/docs/akka/current/persistence-journals.html#plugin-tck)


# Usage

To your `build.sbt` add the resolver and dependency:

```sbt
resolvers += Resolver.bintrayRepo("antex", "maven")
libraryDependencies += "ag.rob" %% "akka-persistence-s3" % VERSION
```