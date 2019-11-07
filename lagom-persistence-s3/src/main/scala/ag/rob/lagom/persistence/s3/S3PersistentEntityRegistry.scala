package ag.rob.lagom.persistence.s3

import akka.actor.ActorSystem
import akka.event.Logging
import com.lightbend.lagom.internal.scaladsl.persistence.AbstractPersistentEntityRegistry

class S3PersistentEntityRegistry(system: ActorSystem)
    extends AbstractPersistentEntityRegistry(system) {

  private val log = Logging.getLogger(system, getClass)

  S3ConfigValidator.validateBucket("s3-journal", system.settings.config, log)
}
