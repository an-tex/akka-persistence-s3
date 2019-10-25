package ag.rob.akka.persistence

import akka.Done
import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{MetaHeaders, S3Headers}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class S3Journal extends AsyncWriteJournal {
  val serialization = SerializationExtension(context.system)
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = context.dispatcher

  val bucket = context.system.settings.config.getString("s3-journal.bucket")

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]) = {
    Future.sequence(messages.groupBy(_.persistenceId).map { case (persistenceId: String, writes: Seq[AtomicWrite]) =>
      Source.fromIterator(() => writes.iterator).flatMapMerge(1, { atomicWrite =>
        if (atomicWrite.payload.length > 1) throw new UnsupportedOperationException

        val persistentRepr = atomicWrite.payload.head
        val payloadAnyRef = persistentRepr.payload.asInstanceOf[AnyRef]
        val serializer = serialization.findSerializerFor(payloadAnyRef)
        val bytes = serializer.toBinary(payloadAnyRef)

        S3.putObject(
          bucket,
          s"${persistentRepr.persistenceId}/${persistentRepr.sequenceNr}",
          Source.single(ByteString(bytes)),
          bytes.length,
          s3Headers = S3Headers().withMetaHeaders(MetaHeaders(Map(
            "persistenceId" -> persistentRepr.persistenceId,
            "sequenceNr" -> persistentRepr.sequenceNr.toString,
            "manifest" -> persistentRepr.manifest,
            "writeUuid" -> persistentRepr.writerUuid,
            "deleted" -> persistentRepr.deleted.toString,
            "serializerId" -> serializer.identifier.toString
          )))
        )
      }).concat(S3
        .putObject(bucket, s"$persistenceId/$metaDataKey", Source.empty, 0, s3Headers = S3Headers().withMetaHeaders(MetaHeaders(Map("highestSequenceNr" -> writes.foldLeft(0L)((previous, atomicWrite) => Math.max(previous, atomicWrite.highestSequenceNr)).toString)))))
        .runWith(Sink.ignore)
    }.toSeq).map(_ => Nil)
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long) = {
    S3
      .listBucket(bucket, Some(persistenceId))
      .filter { listBucketResultsContent =>
        if (listBucketResultsContent.key.endsWith(metaDataKey)) false
        else {
          val _ :: sequenceNr :: Nil = listBucketResultsContent.key.split('/').toList
          val sequenceNrLong = sequenceNr.toLong
          sequenceNrLong <= toSequenceNr
        }
      }
      .mapAsync(1)(content =>
        S3.deleteObject(bucket, content.key).runForeach(_ => Done)
      )
      .runWith(Sink.ignore)
      .map(_ => ())
  }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(recoveryCallback: PersistentRepr => Unit) = {
    S3
      .listBucket(bucket, Some(persistenceId))
      .filter { listBucketResultsContent =>
        if (listBucketResultsContent.key.endsWith(metaDataKey)) false
        else {
          val _ :: sequenceNr :: Nil = listBucketResultsContent.key.split('/').toList
          val sequenceNrLong = sequenceNr.toLong
          sequenceNrLong >= fromSequenceNr && sequenceNrLong <= toSequenceNr
        }
      }
      .take(max)
      .mapAsync(1) { content =>
        S3.download(bucket, content.key).mapAsync(1) { maybeContent =>
          maybeContent.map { case (source, metaData) =>
            source.runForeach { payload =>
              val serializerId = metaData.metadata.find(_.is("x-amz-meta-serializerid")).get.value().toInt
              val manifest = metaData.metadata.find(_.is("x-amz-meta-manifest")).get.value()
              val deserialized = serialization.deserializeByteBuffer(payload.asByteBuffer, serializerId, manifest)
              val repr = PersistentRepr(
                deserialized,
                metaData.metadata.find(_.is("x-amz-meta-sequencenr")).get.value().toLong,
                persistenceId,
                manifest,
                deleted = metaData.metadata.find(_.is("x-amz-meta-deleted")).get.value().toBoolean,
                writerUuid = metaData.metadata.find(_.is("x-amz-meta-writeuuid")).get.value()
              )
              recoveryCallback(repr)
            }
          }.getOrElse(Future.successful(Done))
        }.runWith(Sink.ignore)
      }.runWith(Sink.ignore)
      .map(_ => ())
  }

  val metaDataKey = "0_METADATA"

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long) = {
    S3.getObjectMetadata(bucket, s"$persistenceId/$metaDataKey").runWith(Sink.head).map(_.fold(0L)(_.metadata.find(_.is("x-amz-meta-highestsequencenr")).get.value().toLong))
  }
}
