package ag.rob.akka.persistence

import ag.rob.akka.persistence.scaladsl.S3ReadJournal
import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import com.typesafe.config.Config

class S3ReadJournalProvider(system: ExtendedActorSystem, config: Config) extends ReadJournalProvider {

  override val scaladslReadJournal: S3ReadJournal =
    new S3ReadJournal(system, config)

  override val javadslReadJournal =
    new ag.rob.akka.persistence.javadsl.S3ReadJournal(scaladslReadJournal)
}
