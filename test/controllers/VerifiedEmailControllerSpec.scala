/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import actionbuilders.CustomAuthConnector
import connectors.Sub09Connector
import models.repositories.{FailedToRetrieveEmail, SuccessfulEmail}
import models.{NotificationEmail, TraderData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verifyNoInteractions, when}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import repositories.EmailRepository
import utils.TestData.TEST_EORI_VALUE
import utils.{MockAuthConnector, SpecBase}

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class VerifiedEmailControllerSpec extends SpecBase with MockAuthConnector {

  "getVerifiedEmail" should {

    "return Not Found if no data is found in the cache and SUB09 returns no email" in new Setup {
      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailRoute)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return the email and not call SUB09 if the data is stored in the cache" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(Some(NotificationEmail(testAddress, testTime, None))))

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))
      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailRoute)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("address" -> testAddress, "timestamp" -> s"${testTime.toString}Z")
        verifyNoInteractions(mockSubscriptionInfoService)
      }
    }

    "return the email and call SUB09 if the data is not stored in the cache and " +
      "also store the response into the cache" in new Setup {
        when(mockEmailRepository.get(any()))
          .thenReturn(Future.successful(Some(NotificationEmail(testAddress, testTime, None))))

        when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
          Future.successful(
            Some(NotificationEmail(testAddress, testTime, None))
          )
        )

        when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailRoute)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          contentAsJson(result) mustBe Json.obj("address" -> testAddress, "timestamp" -> s"${testTime.toString}Z")
        }
      }

    "return InternalServerError if the write did not succeed when retrieving email from SUB09" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
        Future.successful(
          Some(NotificationEmail(testAddress, testTime, None))
        )
      )

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(FailedToRetrieveEmail))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailRoute)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "getVerifiedEmailV2" should {

    "return Not Found if no data is found in the cache and SUB09 returns no email" in new Setup {
      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailV2Route)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return the email and not call SUB09 if the data is stored in the cache" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(Some(NotificationEmail(testAddress, testTime, None))))

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))
      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailV2Route)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("address" -> testAddress, "timestamp" -> s"${testTime.toString}Z")
        verifyNoInteractions(mockSubscriptionInfoService)
      }
    }

    "return the email and call SUB09 if the data is not stored in the cache and " +
      "also store the response into the cache" in new Setup {
        when(mockEmailRepository.get(any()))
          .thenReturn(Future.successful(None))

        when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
          Future.successful(
            Some(NotificationEmail(testAddress, testTime, None))
          )
        )

        when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailV2Route)

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          contentAsJson(result) mustBe Json.obj("address" -> testAddress, "timestamp" -> s"${testTime.toString}Z")
        }
      }

    "return InternalServerError if the write did not succeed when retrieving email from SUB09" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
        Future.successful(
          Some(NotificationEmail(testAddress, testTime, None))
        )
      )

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(FailedToRetrieveEmail))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getVerifiedEmailV2Route)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "updateVerifiedEmail" should {

    "return internal server error if the update failed to populate the cache" in new Setup {
      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(FailedToRetrieveEmail))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj("eori" -> TEST_EORI_VALUE, "address" -> testAddress, "timestamp" -> testTime.toString)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 400 with malformed request" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj("invalidKey" -> TEST_EORI_VALUE, "address" -> testAddress)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

    "return 204 if the update was successful with a timestamp present" in new Setup {
      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj("eori" -> TEST_EORI_VALUE, "address" -> testAddress, "timestamp" -> testTime.toString)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NO_CONTENT
      }
    }
  }

  "retrieveVerifiedEmailThirdParty" should {

    "return Not Found if no data is found in the cache and SUB09 returns no email" in new Setup {
      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, retrieveVerifiedEmailThirdPartyRoute).withJsonBody(
        Json.obj("eori" -> TEST_EORI_VALUE)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return the email and not call SUB09 if the data is stored in the cache" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(Some(NotificationEmail(testAddress, testTime, None))))

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))
      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, retrieveVerifiedEmailThirdPartyRoute).withJsonBody(
        Json.obj("eori" -> TEST_EORI_VALUE)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("address" -> testAddress, "timestamp" -> s"${testTime.toString}Z")
        verifyNoInteractions(mockSubscriptionInfoService)
      }
    }

    "return the email and call SUB09 if the data is not stored in the cache and " +
      "also store the response into the cache" in new Setup {
        when(mockEmailRepository.get(any()))
          .thenReturn(Future.successful(Some(NotificationEmail(testAddress, testTime, None))))

        when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
          Future.successful(
            Some(NotificationEmail(testAddress, testTime, None))
          )
        )

        when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

        val request: FakeRequest[AnyContentAsJson] =
          FakeRequest(POST, retrieveVerifiedEmailThirdPartyRoute).withJsonBody(
            Json.obj("eori" -> TEST_EORI_VALUE)
          )

        running(app) {
          val result = route(app, request).value
          status(result) mustBe OK
          contentAsJson(result) mustBe Json.obj("address" -> testAddress, "timestamp" -> s"${testTime.toString}Z")
        }
      }

    "return InternalServerError if the write did not succeed when retrieving email from SUB09" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
        Future.successful(
          Some(NotificationEmail(testAddress, testTime, None))
        )
      )

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(FailedToRetrieveEmail))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, retrieveVerifiedEmailThirdPartyRoute).withJsonBody(
        Json.obj("eori" -> TEST_EORI_VALUE)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 400 with malformed request" in new Setup {
      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoService.getSubscriberInformation(any())).thenReturn(
        Future.successful(
          Some(NotificationEmail(testAddress, testTime, None))
        )
      )

      when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, retrieveVerifiedEmailThirdPartyRoute).withJsonBody(
        Json.obj("invalidkey" -> TEST_EORI_VALUE)
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

  }

  trait Setup {
    val testTime1: LocalDate    = LocalDate.now()
    val testTime: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    val testAddress: String     = "test@email.com"

    val getVerifiedEmailRoute: String                = routes.VerifiedEmailController.getVerifiedEmail(TEST_EORI_VALUE).url
    val getVerifiedEmailV2Route: String              = routes.VerifiedEmailController.getVerifiedEmailV2().url
    val postRoute: String                            = routes.VerifiedEmailController.updateVerifiedEmail().url
    val retrieveVerifiedEmailThirdPartyRoute: String =
      routes.VerifiedEmailController.retrieveVerifiedEmailThirdParty().url

    val testNotificationEmail: NotificationEmail = NotificationEmail(testAddress, testTime, None)
    val testTraderData: TraderData               = TraderData(Seq.empty, Some(testNotificationEmail))

    val mockEmailRepository: EmailRepository        = mock[EmailRepository]
    val mockSubscriptionInfoService: Sub09Connector = mock[Sub09Connector]

    def app: Application = application
      .overrides(
        inject.bind[CustomAuthConnector].toInstance(mockAuthConnector),
        inject.bind[EmailRepository].toInstance(mockEmailRepository),
        inject.bind[Sub09Connector].toInstance(mockSubscriptionInfoService)
      )
      .build()
  }
}
