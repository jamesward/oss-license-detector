import java.util.concurrent.TimeUnit

import com.ning.http.client.AsyncHttpClient
import org.scalatest._
import play.api.libs.ws.WS
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import scala.concurrent.ExecutionContext.Implicits.global

class ServerSpec extends PlaySpec with OneServerPerSuite {

  "large licenses" must {
    "work" in {

      val ws = WS.client

      val license = await(ws.url("http://www.tinymce.com/license").get().map(_.body))

      val response = await(ws.url(s"http://localhost:$port").post(license), 90, TimeUnit.SECONDS)

      ws.underlying[AsyncHttpClient].close()

      response.status mustBe OK
      response.body mustBe "LGPL-2.1"
    }
  }

}