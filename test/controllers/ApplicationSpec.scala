package controllers

import java.util.concurrent.TimeUnit

import play.api.http.Status
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext


class ApplicationSpec extends PlaySpec with GuiceOneAppPerTest {

  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

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

      val applicationController = app.injector.instanceOf[Application]

      val result = applicationController.license(FakeRequest("POST", "/", FakeHeaders(), license))

      status(result) must be (Status.OK)
      contentAsString(result) must equal ("BSD 3-Clause")
    }
  }

  "initial response" must {
    "happen in under 30 seconds" in {
      val ws = app.injector.instanceOf[WSClient]

      val applicationController = app.injector.instanceOf[Application]

      val future = ws.url("https://raw.githubusercontent.com/tinymce/tinymce/master/LICENSE.TXT").get().flatMap { response =>
        applicationController.license(FakeRequest("POST", "/", FakeHeaders(), response.body))
      }

      await(future, 30, TimeUnit.SECONDS).header.status must (equal (Status.SEE_OTHER) or equal (Status.OK))
    }
  }

}
