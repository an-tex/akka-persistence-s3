package ag.rob.akka.persistence

import akka.persistence.journal.{AsyncWriteJournal, Tagged}
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.{SerializationExtension, Serializers}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{MetaHeaders, S3Headers}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.collection.immutable
import scala.concurrent.Future

class S3Journal extends AsyncWriteJournal {
  val serialization = SerializationExtension(context.system)

  import context.{dispatcher, system}

  private val bucket = context.system.settings.config.getString("s3-journal.bucket")
  private val writeParallelism = context.system.settings.config.getInt("s3-journal.parallelism.write")
  private val readParallelism = context.system.settings.config.getInt("s3-journal.parallelism.read")

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]) = {
    Source(messages)
      .groupBy(Int.MaxValue, _.persistenceId)
      .flatMapMerge(1, atomicWrite => {
        if (atomicWrite.payload.length > 1) throw new UnsupportedOperationException

        val persistentRepr = atomicWrite.payload.head
        val payload = persistentRepr.payload match {
          case Tagged(payload, _) => payload
          case p => p
        }
        val payloadAnyRef = payload.asInstanceOf[AnyRef]
        val serializer = serialization.findSerializerFor(payloadAnyRef)
        val serializerManifest = Serializers.manifestFor(serializer, payloadAnyRef)
        val bytes = serializer.toBinary(payloadAnyRef)

        S3.putObject(
          bucket,
          s"${persistentRepr.persistenceId.replaceAllLiterally("|", "/")}/${f"${persistentRepr.sequenceNr}%019d"}",
          Source.single(ByteString(bytes)),
          bytes.length,
          s3Headers = S3Headers().withMetaHeaders(MetaHeaders(Map(
            "persistenceId" -> persistentRepr.persistenceId,
            "sequenceNr" -> persistentRepr.sequenceNr.toString,
            "manifest" -> persistentRepr.manifest,
            "serializerManifest" -> serializerManifest,
            "writeUuid" -> persistentRepr.writerUuid,
            "deleted" -> persistentRepr.deleted.toString,
            "serializerId" -> serializer.identifier.toString
          )))
        )
      })
      .mergeSubstreamsWithParallelism(writeParallelism)
      .runWith(Sink.ignore)
      .map(_ => Nil)
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long) = S3
    .listBucket(bucket, Some(persistenceId.replaceAllLiterally("|", "/")))
    .filter(listBucketResultsContent => extractSequenceNr(listBucketResultsContent.key) <= toSequenceNr)
    .flatMapConcat(content =>
      S3.deleteObject(bucket, content.key).map(_ => content.key)
    )
    .runWith(Sink.lastOption)
    .flatMap(maybeLastKey =>
      maybeLastKey.fold(Future.successful(())) { lastKey =>
        val key = s"${persistenceId.replaceAllLiterally("|", "/")}/${f"${extractSequenceNr(lastKey)}%019d"}$sequenceNrMakerSuffix"
        S3.putObject(bucket, key, Source.empty, 0L, s3Headers = S3Headers.empty).runWith(Sink.ignore).map(_ => ())
      }
    )

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(recoveryCallback: PersistentRepr => Unit) = {
    S3
      .listBucket(bucket, Some(persistenceId.replaceAllLiterally("|", "/")))
      .filter { listBucketResultsContent =>
        if (isHighestSequenceNrMarker(listBucketResultsContent.key)) false
        else {
          val sequenceNr = extractSequenceNr(listBucketResultsContent.key)
          sequenceNr >= fromSequenceNr && sequenceNr <= toSequenceNr
        }
      }
      .take(max)
      .mapAsync(readParallelism) { content =>
        S3
          .download(bucket, content.key)
          .collect {
            case Some(elem) => elem
          }
          .mapAsync(1) { case (source, metaData) =>
            source
              .runWith(Sink.reduce[ByteString](_ ++ _))
              .map { payload =>
                val serializerId = metaData.metadata.find(_.is("x-amz-meta-serializerid")).get.value().toInt
                val deserialized = serialization.deserializeByteBuffer(payload.asByteBuffer, serializerId, metaData.metadata.find(_.is("x-amz-meta-serializermanifest")).get.value())
                PersistentRepr(
                  deserialized,
                  metaData.metadata.find(_.is("x-amz-meta-sequencenr")).get.value().toLong,
                  persistenceId,
                  metaData.metadata.find(_.is("x-amz-meta-manifest")).get.value(),
                  deleted = metaData.metadata.find(_.is("x-amz-meta-deleted")).get.value().toBoolean,
                  writerUuid = metaData.metadata.find(_.is("x-amz-meta-writeuuid")).get.value()
                )
              }
          }
          .runWith(Sink.head)
      }
      .runWith(Sink.foreach(recoveryCallback))
      .map(_ => ())
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long) = {
    S3
      .listBucket(bucket, Some(persistenceId.replaceAllLiterally("|", "/")))
      .map(_.key)
      .runWith(Sink.lastOption)
      .map(_.fold(0L)(extractSequenceNr))
  }

  private val sequenceNrMakerSuffix = "_SEQNR_MARKER"

  private def isHighestSequenceNrMarker(key: String) = key.endsWith(sequenceNrMakerSuffix)

  private def extractSequenceNr(key: String) = {
    val lastObject = key.split('/').toList.last
    val sequenceNr =
      if (isHighestSequenceNrMarker(key)) lastObject.split('_').head
      else lastObject
    sequenceNr.toLong
  }
}
