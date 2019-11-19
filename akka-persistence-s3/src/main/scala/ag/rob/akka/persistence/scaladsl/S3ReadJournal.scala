package ag.rob.akka.persistence.scaladsl

import akka.NotUsed
import akka.actor.ExtendedActorSystem
import akka.persistence.query.scaladsl.{CurrentPersistenceIdsQuery, ReadJournal}
import akka.stream.alpakka.s3.ListBucketResultCommonPrefixes
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import com.typesafe.config.Config

class S3ReadJournal(system: ExtendedActorSystem, config: Config)
  extends ReadJournal with CurrentPersistenceIdsQuery {

  val bucket = system.settings.config.getString("s3-journal.bucket")

  override def currentPersistenceIds(): Source[String, NotUsed] = S3.listObjects(bucket, delimiter = Some("/")).collect {
    case prefixes: ListBucketResultCommonPrefixes => prefixes.prefix
  }.flatMapConcat(prefix => S3.listObjects(bucket, prefix = Some(prefix), delimiter = Some("/"))).collect {
    case prefixes: ListBucketResultCommonPrefixes => prefixes.prefix.stripSuffix("/").replaceAllLiterally("/","|")
  }
}
