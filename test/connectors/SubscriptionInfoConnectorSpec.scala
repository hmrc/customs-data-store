/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import models.Sub09Response
import models.responses.MdgSub09Response
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers.running
import utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, ServiceUnavailableException}

import java.net.URL
import scala.concurrent.Future


class SubscriptionInfoConnectorSpec extends SpecBase {

  "getSubscriberInformation" should {

    "return None when the timestamp is not available" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailNoTimestamp(testEori))))

      running(app) {
        await(service.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "return Some, when the timestamp is available" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailAndTimestamp(testEori))))

      running(app) {
        val result = await(service.getSubscriberInformation(testEori)).value
        result.emailAddress.value mustBe "mickey.mouse@disneyland.com"
        result.verifiedTimestamp.nonEmpty mustBe true
      }
    }

    "return None when the email is not available" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.noEmailNoTimestamp(testEori))))

      running(app) {
        await(service.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "propagate ServiceUnavailableException" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      running(app) {
        assertThrows[ServiceUnavailableException](await(service.getSubscriberInformation(testEori)))
      }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val testEori = "GB1234567890"
    def mdgResponse(value: JsValue): MdgSub09Response = MdgSub09Response.sub09Reads.reads(value).get
    val mockHttp = mock[HttpClient]
    val app = application.overrides(
      bind[HttpClient].toInstance(mockHttp)
    ).build()

    val service = app.injector.instanceOf[SubscriptionInfoConnector]
  }
}
