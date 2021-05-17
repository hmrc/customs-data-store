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

package uk.gov.hmrc.customs.datastore.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.datastore.domain.onwire.{MdgSub09DataModel, Sub09Response}
import uk.gov.hmrc.customs.datastore.utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, ServiceUnavailableException}

import scala.concurrent.Future


class SubscriptionInfoServiceSpec extends SpecBase {



  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val testEori = "GB1234567890"
    def mdgResponse(value: JsValue): MdgSub09DataModel = MdgSub09DataModel.sub09Reads.reads(value).get
  }

  "getSubscriberInformation" should {

    "return None when the timestamp is not available" in new Setup {
      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[MdgSub09DataModel](any())(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailNoTimestamp(testEori))))

      private val app = application.overrides(
        bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[SubscriptionInfoService]

      running(app) {
        await(service.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "return Some, when the timestamp is available" in new Setup {
      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[MdgSub09DataModel](any())(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailAndTimestamp(testEori))))

      private val app = application.overrides(
        bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[SubscriptionInfoService]

      running(app) {
        val result = await(service.getSubscriberInformation(testEori)).value
        result.emailAddress.value mustBe "mickey.mouse@disneyland.com"
        result.verifiedTimestamp.nonEmpty mustBe true
      }
    }

    "return None when the email is not available" in new Setup {

      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[MdgSub09DataModel](any())(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.noEmailNoTimestamp(testEori))))

      private val app = application.overrides(
        bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[SubscriptionInfoService]

      running(app) {
        await(service.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "propagate ServiceUnavailableException" in new Setup {
      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[MdgSub09DataModel](any())(any(), any(), any()))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      private val app = application.overrides(
        bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[SubscriptionInfoService]

      running(app) {
        assertThrows[ServiceUnavailableException](await(service.getSubscriberInformation(testEori)))
      }
    }
  }
}
