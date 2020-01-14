package ag.rob.akka.persistence

import akka.persistence.journal.JournalPerfSpec
import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore

import scala.concurrent.duration._

@Ignore
class S3JournalPerfSpec extends JournalPerfSpec(S3JournalSpec.minioConfig.withFallback(ConfigFactory.parseString(
  """
    |akka.actor.serialization-bindings {
    |  "akka.persistence.journal.JournalPerfSpec$Cmd" = jackson-json
    |}
    |akka.http.host-connection-pool {
    |  max-connections = 32
    |  min-connections = 4
    |  max-open-requests = 128
    |}
    |s3-journal {
    |  parallelism {
    |    write = 8
    |    read = 32
    |  }
    |  circuit-breaker {
    |    max-failures = 10
    |    call-timeout = 60s
    |    reset-timeout = 30s
    |  }
    |}
    |""".stripMargin))) {
  override def supportsRejectingNonSerializableObjects = false

  override def supportsSerialization = false

  override def supportsAtomicPersistAllOfSeveralEvents = false

  override def awaitDurationMillis = 5.minute.toMillis

  override def eventsCount = 10 * 1000

  protected override def beforeAll() = {
    super.beforeAll()
    S3JournalSpec.beforeAll()
  }

  protected override def afterAll() = {
    //S3JournalSpec.afterAll()
    super.afterAll()
  }
}
