package ag.rob.akka.persistence.scaladsl

import akka.NotUsed
import akka.actor.ExtendedActorSystem
import akka.persistence.query.scaladsl.{CurrentPersistenceIdsQuery, ReadJournal}
import akka.stream.alpakka.s3.S3Attributes
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import com.typesafe.config.Config

import scala.collection.compat._

class S3ReadJournal(system: ExtendedActorSystem, config: Config)
  extends ReadJournal with CurrentPersistenceIdsQuery {

  val bucket = system.settings.config.getString("s3-journal.bucket")
  val s3Attributes = S3Attributes.settingsPath("s3-journal.alpakka.s3")

  override def currentPersistenceIds(): Source[String, NotUsed] =
    S3
    .listBucketAndCommonPrefixes(bucket, "/").withAttributes(s3Attributes)
    .mapConcat(_._2.to(scala.collection.immutable.Iterable))
    .flatMapConcat(prefix => S3.listBucketAndCommonPrefixes(bucket, "/", prefix = Some(prefix.prefix)))
    .mapConcat(_._2.to(scala.collection.immutable.Iterable))
    .map(_.prefix.stripSuffix("/").replaceAllLiterally("/", "|"))
}
