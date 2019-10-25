package ag.rob.akka.persistence

import java.util.Properties

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.adobe.testing.s3mock.S3MockApplication
import com.typesafe.config.ConfigFactory
import org.gaul.shaded.org.eclipse.jetty.util.component.AbstractLifeCycle

import scala.util.Random

class S3JournalSpec extends JournalSpec(
  config = ConfigFactory.parseString(
    s"""
       |akka.persistence.journal.plugin = "s3-journal"
       |alpakka.s3 {
       |  aws {
       |    credentials {
       |      provider = anon
       |    }
       |  }
       |  #endpoint-url = "http://localhost:${S3Mock.application.getHttpPort}"
       |  endpoint-url = "http://localhost:${S3Mock.server.getPort}"
       |}
       |s3-journal.bucket = "${S3Mock.bucket}"
       |""".stripMargin).withFallback(ConfigFactory.load())) {

  override def supportsRejectingNonSerializableObjects = CapabilityFlag.off

  override def supportsSerialization = CapabilityFlag.off

  protected override def beforeAll() = {
    super.beforeAll()
  }

  protected override def afterAll() = {
    super.afterAll()
    S3Mock.application.stop()
  }
}

object S3Mock {
  val bucket = s"akka-persistence-s3-${Random.alphanumeric.take(4).mkString}"

  val application = S3MockApplication.start("--server.port=0", s"--initialBuckets=$bucket")

  val server = {
    import java.net.URI

    import org.gaul.s3proxy.S3Proxy
    import org.jclouds.ContextBuilder
    import org.jclouds.blobstore.BlobStoreContext

    val properties = new Properties()

    val context = ContextBuilder.newBuilder("transient").credentials("identity", "credential").overrides(properties).build(classOf[BlobStoreContext])

    context.getBlobStore.createContainerInLocation(null, bucket)

    val s3Proxy = S3Proxy.builder.blobStore(context.getBlobStore).endpoint(URI.create(s"http://localhost:${Random.nextInt(30000) + 30000}")).build

    s3Proxy.start()
    while ( {
      !(s3Proxy.getState == AbstractLifeCycle.STARTED)
    }) Thread.sleep(1)
    s3Proxy
  }
}

