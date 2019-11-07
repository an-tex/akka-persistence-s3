package ag.rob.lagom.persistence.s3

import akka.event.LoggingAdapter
import com.typesafe.config.Config

object S3ConfigValidator {

  def validateBucket(namespace: String, config: Config, log: LoggingAdapter): Unit =
    if (log.isErrorEnabled) {
      val bucketPath = s"$namespace.bucket"
      if (!config.hasPath(bucketPath)) {
        log.error("Configuration for [{}] must be set in application.conf ", bucketPath)
      }
    }
}
