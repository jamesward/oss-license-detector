import java.util.concurrent.TimeUnit

import play.api.test.Helpers._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext


class ServerSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  "large licenses" must {
    "work" in {

      val ws = app.injector.instanceOf[WSClient]

      val license = await(ws.url("http://www.tinymce.com/license").get().map(_.body))

      val response = await(ws.url(s"http://localhost:$port").post(license), 90, TimeUnit.SECONDS)

      response.status mustBe OK
      response.body mustBe "LGPL-2.1"
    }
  }

}
