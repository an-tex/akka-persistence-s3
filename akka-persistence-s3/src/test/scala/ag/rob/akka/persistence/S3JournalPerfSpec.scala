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
    |""".stripMargin))) {
  override def supportsRejectingNonSerializableObjects = false

  override def supportsSerialization = false

  override def supportsAtomicPersistAllOfSeveralEvents = false

  override def awaitDurationMillis = 1.minute.toMillis

  override def eventsCount = 1010

  protected override def beforeAll() = {
    super.beforeAll()
    S3JournalSpec.beforeAll()
  }

  protected override def afterAll() = {
    S3JournalSpec.afterAll()
    super.afterAll()
  }
}
