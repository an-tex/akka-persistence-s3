package ag.rob.akka.persistence

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.persistence.journal.JournalSpec
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class S3JournalSpec extends JournalSpec(S3JournalSpec.minioConfig) {

  override def supportsRejectingNonSerializableObjects = false

  override def supportsSerialization = false

  override def supportsAtomicPersistAllOfSeveralEvents = false

  protected override def beforeAll() = {
    super.beforeAll()
    S3JournalSpec.beforeAll()
  }

  protected override def afterAll() = {
    S3JournalSpec.afterAll()
    super.afterAll()
  }
}

object S3JournalSpec {
  val testBucket = {
    val nowS3Compatible = LocalDateTime.now()
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      .replaceAllLiterally(":", "-")
      .replaceAllLiterally(".", "-")

    s"akka-persistence-s3-spec-${nowS3Compatible}-${Random.alphanumeric.take(4).mkString}".toLowerCase
  }

  val minioConfig = ConfigFactory.parseString(
    s"""
       |akka.persistence.journal.plugin = "s3-journal"
       |alpakka.s3 {
       |  aws {
       |    credentials {
       |      provider = static
       |      access-key-id = "minio"
       |      secret-access-key = "minio123"
       |    }
       |    region {
       |      provider = static
       |      default-region = "eu-central-1"
       |    }
       |  }
       |  endpoint-url = "http://127.0.0.1:9876"
       |}
       |s3-journal.bucket = "$testBucket"
       |""".stripMargin).withFallback(ConfigFactory.load())

  def beforeAll()(implicit actorSystem: ActorSystem) = {
    Await.result(S3.makeBucket(S3JournalSpec.testBucket), 5.seconds)
  }

  def afterAll()(implicit actorSystem: ActorSystem) = {
    val cleanup = S3
      .listBucket(S3JournalSpec.testBucket, None)
      .flatMapConcat(contents =>
        S3.deleteObject(S3JournalSpec.testBucket, contents.key)
      )
      .runWith(Sink.ignore)
      .flatMap(_ =>
        S3.deleteBucket(S3JournalSpec.testBucket)
      )
    //wait longer in case it's used in a performance test
    Await.result(cleanup, 2.minute)
  }
}
