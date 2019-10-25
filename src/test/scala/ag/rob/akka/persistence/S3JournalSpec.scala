package ag.rob.akka.persistence

import java.net.URI
import java.util.Properties

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory
import org.gaul.s3proxy.S3Proxy
import org.gaul.shaded.org.eclipse.jetty.util.component.AbstractLifeCycle
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext

import scala.util.Random

class S3JournalSpec extends JournalSpec(
  config = ConfigFactory.parseString(
    s"""
       |akka.persistence.journal.plugin = "s3-journal"
       |alpakka.s3 {
       |  aws.credentials.provider = anon
       |  endpoint-url = "http://localhost:${S3Mock.port}"
       |}
       |s3-journal.bucket = "${S3Mock.bucket}"
       |""".stripMargin).withFallback(ConfigFactory.load())) {

  override def supportsRejectingNonSerializableObjects = CapabilityFlag.off

  override def supportsSerialization = CapabilityFlag.off

  protected override def beforeAll() = {
    super.beforeAll()
    S3Mock.start()
  }

  protected override def afterAll() = {
    super.afterAll()
    S3Mock.stop()
  }
}

object S3Mock {
  val bucket = s"akka-persistence-s3-${Random.alphanumeric.take(4).mkString}"

  lazy val port = Random.nextInt(30000) + 30000

  lazy private[this] val s3Proxy = {
    val context = ContextBuilder
      .newBuilder("transient")
      .credentials("identity", "credential")
      .overrides(new Properties())
      .build(classOf[BlobStoreContext])
    context.getBlobStore.createContainerInLocation(null, bucket)
    S3Proxy.builder.blobStore(context.getBlobStore).endpoint(URI.create(s"http://localhost:$port")).build
  }

  def start() = {
    s3Proxy.start()
    while ( {
      !(s3Proxy.getState == AbstractLifeCycle.STARTED)
    }) Thread.sleep(1)
    s3Proxy
  }

  def stop() = s3Proxy.stop()
}

