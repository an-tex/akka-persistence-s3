package ag.rob.akka.persistence

import akka.Done
import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.SerializationExtension
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.MetaHeaders
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class S3Journal extends AsyncWriteJournal {
  val serialization = SerializationExtension(context.system)
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = context.dispatcher

  val bucket = "akka-s3-persistence"

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]) = {
    Future.sequence(messages.flatMap { message =>
      message.payload.map { persistentRepr =>
        val payloadAnyRef = persistentRepr.payload.asInstanceOf[AnyRef]
        val serializer = serialization.findSerializerFor(payloadAnyRef)
        val bytes = serializer.toBinary(payloadAnyRef)
        val sink = S3.multipartUpload(
          bucket,
          s"${persistentRepr.persistenceId}/${persistentRepr.sequenceNr}",
          metaHeaders = MetaHeaders(Map(
            "persistenceId" -> persistentRepr.persistenceId,
            "sequenceNr" -> persistentRepr.sequenceNr.toString,
            "manifest" -> persistentRepr.manifest,
            "writeUuid" -> persistentRepr.writerUuid,
            "deleted" -> persistentRepr.deleted.toString,
            "serializerId" -> serializer.identifier.toString
          )
          )
        )
        Source
          .single(ByteString(bytes))
          .runWith(sink)
          .map(_ => Success())
      }
    })
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long) = ???

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(recoveryCallback: PersistentRepr => Unit) = {
    S3.listBucket(bucket, Some(persistenceId)).mapAsync(1) { content =>
      val persistenceId :: sequenceNr :: Nil = content.key.split('/').toList
      val sequenceNrLong = sequenceNr.toLong
      if (sequenceNrLong >= fromSequenceNr && sequenceNrLong <= toSequenceNr) {
        S3.download(bucket, content.key).mapAsync(1) { maybeContent =>
          maybeContent.map { case (source, metaData) =>
            source.runForeach { payload =>
              val serializerId = metaData.metadata.find(_.is("x-amz-meta-serializerid")).get.value().toInt
              val manifest = metaData.metadata.find(_.is("x-amz-meta-manifest")).get.value()
              val deserialized = serialization.deserializeByteBuffer(payload.asByteBuffer, serializerId, manifest)
              val repr = PersistentRepr(
                deserialized,
                sequenceNrLong,
                persistenceId,
                manifest,
                deleted = metaData.metadata.find(_.is("x-amz-meta-deleted")).get.value().toBoolean,
                writerUuid = metaData.metadata.find(_.is("x-amz-meta-writeuuid")).get.value()
              )
              println(s"got payload $sequenceNrLong: $repr")
              recoveryCallback(repr)
            }
          }.getOrElse(Future.successful(Done))
        }.runForeach(_ => Done)
      } else Future.successful(Done)
    }.runForeach { _ =>
      Done
    }.map { _ =>
      ()
    }
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long) = {
    S3.listBucket(bucket, Some(persistenceId)).runFold(0L) { case (previous, content) =>
      val (_, sequenceNr) = content.key.splitAt(content.key.indexOf("/") + 1)
      val sequenceNrLong = sequenceNr.toLong

      if (sequenceNr.toLong > previous) sequenceNrLong
      else previous
    }
  }
}
