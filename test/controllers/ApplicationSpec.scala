package controllers

import java.util.concurrent.{Executors, TimeUnit}

import com.ning.http.client.AsyncHttpClient
import play.api.http.Status
import play.api.libs.concurrent.Akka
import play.api.libs.ws.WS
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

import scala.concurrent.{ExecutionContext, Future}

class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Application.license" must {
    "detect the BSD 3-Clause license for http://polymer.github.io/LICENSE.txt" in {
      val license =
        """// Copyright (c) 2014 The Polymer Authors. All rights reserved.
          |//
          |// Redistribution and use in source and binary forms, with or without
          |// modification, are permitted provided that the following conditions are
          |// met:
          |//
          |//    * Redistributions of source code must retain the above copyright
          |// notice, this list of conditions and the following disclaimer.
          |//    * Redistributions in binary form must reproduce the above
          |// copyright notice, this list of conditions and the following disclaimer
          |// in the documentation and/or other materials provided with the
          |// distribution.
          |//    * Neither the name of Google Inc. nor the names of its
          |// contributors may be used to endorse or promote products derived from
          |// this software without specific prior written permission.
          |//
          |// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
          |// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
          |// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
          |// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
          |// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
          |// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
          |// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
          |// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
          |// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
          |// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
          |// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.""".stripMargin


      val result = controllers.Application.license(None)(FakeRequest("POST", "/", FakeHeaders(), license))

      status(result) must be (Status.OK)
      contentAsString(result) must equal ("BSD 3-Clause")
    }
  }

  "initial response" must {
    "happen in under 25 seconds" in {
      implicit val ec = play.api.libs.concurrent.Execution.defaultContext

      val ws = WS.client
      val future = ws.url("http://www.tinymce.com/license").get().flatMap { response =>
        controllers.Application.license(None)(FakeRequest("POST", "/", FakeHeaders(), response.body))
      }
      future.foreach(_ => ws.underlying[AsyncHttpClient].close())
      await(future, 25, TimeUnit.SECONDS).header.status must equal (Status.FOUND)
    }
    "happen in under 25 seconds even when there are a bunch of requests" in {

      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(100))

      val ws = WS.client
      val license = await(ws.url("http://www.tinymce.com/license").get().map(_.body))
      ws.underlying[AsyncHttpClient].close()

      val future = Future.sequence {
        Seq.fill(5) {
          controllers.Application.license(None)(FakeRequest("POST", "/", FakeHeaders(), license))
        }
      }

      await(future, 25, TimeUnit.SECONDS).head.header.status must equal (Status.FOUND)
    }
  }

}