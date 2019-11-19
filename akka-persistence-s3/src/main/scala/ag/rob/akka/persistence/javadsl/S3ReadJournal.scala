package ag.rob.akka.persistence.javadsl

import akka.persistence.query.javadsl._

class S3ReadJournal(scaladslReadJournal: ag.rob.akka.persistence.scaladsl.S3ReadJournal)
  extends ReadJournal with CurrentPersistenceIdsQuery {
  override def currentPersistenceIds() = scaladslReadJournal.currentPersistenceIds().asJava
}
