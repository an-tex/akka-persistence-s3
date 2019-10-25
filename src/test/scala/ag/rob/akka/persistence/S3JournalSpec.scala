package ag.rob.akka.persistence

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory

class S3JournalSpec extends JournalSpec(
  config = ConfigFactory.parseString("""akka.persistence.journal.plugin = "s3-journal"""").withFallback(ConfigFactory.load())) {

  override def supportsRejectingNonSerializableObjects = CapabilityFlag.off

  override def supportsSerialization = CapabilityFlag.off
}
