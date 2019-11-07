package ag.rob.lagom.persistence.s3

import com.lightbend.lagom.scaladsl.persistence.{PersistenceComponents, PersistentEntityRegistry, WriteSidePersistenceComponents}

trait S3PersistenceComponents
  extends PersistenceComponents
    with WriteSideS3PersistenceComponents

trait WriteSideS3PersistenceComponents extends WriteSidePersistenceComponents {

  override lazy val persistentEntityRegistry: PersistentEntityRegistry =
    new S3PersistentEntityRegistry(actorSystem)
}

