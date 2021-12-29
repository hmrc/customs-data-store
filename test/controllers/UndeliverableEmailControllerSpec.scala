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

package controllers

import connectors.Sub22Connector
import models.repositories.{NotificationEmailMongo, UndeliverableInformationMongo}
import models.{UndeliverableInformation, UndeliverableInformationEvent}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.EmailRepository
import services.UndeliverableJobService
import utils.SpecBase

import scala.concurrent.Future

class UndeliverableEmailControllerSpec extends SpecBase {

  "makeUndeliverable" should {
    "return 404 if user has not been found in the data-store based on EORI" in new Setup {
      when(mockEmailRepository.findAndUpdate(any(), any())).thenReturn(Future.successful(None))
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "subject" -> "some subject",
          "eventId" -> "some id",
          "groupId" -> "someGroupId",
          "timestamp" -> DateTime.now().toString(),
          "event" -> Json.obj(
            "id" -> "some-id",
            "enrolment" -> s"HMRC-CUS-ORG~EORINumber~$testEori",
            "emailAddress" -> "some@email.com",
            "event" -> "some event",
            "detected" -> DateTime.now().toString(),
            "code" -> 12,
            "reason" -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
      }
    }

    "return 400 if the enrolment does not contain 'EORINumber'" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(

        Json.obj(
          "subject" -> "some subject",
          "eventId" -> "some id",
          "groupId" -> "someGroupId",
          "timestamp" -> DateTime.now().toString(),
          "event" -> Json.obj(
            "id" -> "some-id",
            "enrolment" -> s"HMRC-CUS-ORG~INVALID~$testEori",
            "emailAddress" -> "some@email.com",
            "event" -> "some event",
            "detected" -> DateTime.now().toString(),
            "code" -> 12,
            "reason" -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

    "return 400 if the enrolment does not contain 'HMRC-CUS-ORG'" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "enrolment" -> s"INVALID~EORINumber~$testEori",
          "emailAddress" -> "some@email.com",
          "event" -> "some event",
          "detected" -> DateTime.now().toString(),
          "code" -> 12,
          "reason" -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

    "return 400 if the data provided to the endpoint is invalid" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "subject" -> "some subject",
          "eventId" -> "some id",
          "groupId" -> "someGroupId",
          "timestamp" -> DateTime.now().toString(),
          "event" -> Json.obj(
            "id" -> "some-id",
            "emailAddress" -> "some@email.com",
            "event" -> "some event",
            "detected" -> DateTime.now().toString(),
            "code" -> 12,
            "reason" -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe BAD_REQUEST
      }
    }

    "return 500 if the update to data to the database failed to write" in new Setup {
      when(mockEmailRepository.findAndUpdate(any(), any())).thenReturn(
        Future.failed(new RuntimeException("something went wrong"))
      )

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "subject" -> "some subject",
          "eventId" -> "some id",
          "groupId" -> "someGroupId",
          "timestamp" -> DateTime.now().toString(),
          "event" -> Json.obj(
            "id" -> "some-id",
            "enrolment" -> s"HMRC-CUS-ORG~EORINumber~$testEori",
            "emailAddress" -> "some@email.com",
            "event" -> "some event",
            "detected" -> DateTime.now().toString(),
            "code" -> 12,
            "reason" -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 204 if the update was successful to the database" in new Setup {
      val detectedDate: DateTime = DateTime.now()

      val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
        "some-id",
        "some event",
        "some@email.com",
        detectedDate.toString(),
        Some(12),
        Some("unknown reason"),
        s"HMRC-cus-ORG~EORINUMBER~$testEori"
      )

      val expectedRequest: UndeliverableInformation =
        UndeliverableInformation(
          "some subject",
          "some event",
          "someGroupId",
          detectedDate,
          undeliverableInformationEvent
        )

      val undeliverableInformationMongo: UndeliverableInformationMongo = UndeliverableInformationMongo(
        "some subject",
        "some event",
        "someGroupId",
        detectedDate,
        undeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )
      val newNotificationEmailMongo: NotificationEmailMongo = NotificationEmailMongo(
        "some@email.com",
        detectedDate,
        Some(undeliverableInformationMongo)
      )

      when(mockEmailRepository.findAndUpdate(any(), any()))
        .thenReturn(Future.successful(Some(newNotificationEmailMongo)))
      when(mockSub22Connector.updateUndeliverable(any(), any(), any())(any()))
        .thenReturn(Future.successful(true))
      when(mockEmailRepository.markAsSuccessful(any()))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "subject" -> "some subject",
          "eventId" -> "some event",
          "groupId" -> "someGroupId",
          "timestamp" -> detectedDate.toString(),
          "event" -> Json.obj(
            "id" -> "some-id",
            "enrolment" -> s"HMRC-cus-ORG~EORINUMBER~$testEori",
            "emailAddress" -> "some@email.com",
            "event" -> "some event",
            "detected" -> detectedDate.toString(),
            "code" -> 12,
            "reason" -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NO_CONTENT
        verify(mockEmailRepository).findAndUpdate(testEori, expectedRequest)
      }
    }

    "return 204 if the update failed to SUB22" in new Setup {
      val detectedDate: DateTime = DateTime.now()

      val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
        "some-id",
        "some event",
        "some@email.com",
        detectedDate.toString(),
        Some(12),
        Some("unknown reason"),
        s"HMRC-cus-ORG~EORINUMBER~$testEori"
      )

      val expectedRequest: UndeliverableInformation =
        UndeliverableInformation(
          "some subject",
          "some event",
          "someGroupId",
          detectedDate,
          undeliverableInformationEvent
        )

      val undeliverableInformationMongo: UndeliverableInformationMongo = UndeliverableInformationMongo(
        "some subject",
        "some event",
        "someGroupId",
        detectedDate,
        undeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )
      val newNotificationEmailMongo: NotificationEmailMongo = NotificationEmailMongo(
        "some@email.com",
        detectedDate,
        Some(undeliverableInformationMongo)
      )

      when(mockEmailRepository.findAndUpdate(any(), any()))
        .thenReturn(Future.successful(Some(newNotificationEmailMongo)))
      when(mockSub22Connector.updateUndeliverable(any(), any(), any())(any()))
        .thenReturn(Future.successful(false))
      when(mockEmailRepository.resetProcessing(any()))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "subject" -> "some subject",
          "eventId" -> "some event",
          "groupId" -> "someGroupId",
          "timestamp" -> detectedDate.toString(),
          "event" -> Json.obj(
            "id" -> "some-id",
            "enrolment" -> s"HMRC-cus-ORG~EORINUMBER~$testEori",
            "emailAddress" -> "some@email.com",
            "event" -> "some event",
            "detected" -> detectedDate.toString(),
            "code" -> 12,
            "reason" -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NO_CONTENT
        verify(mockEmailRepository).findAndUpdate(testEori, expectedRequest)
      }
    }

  }

  trait Setup {
    val notificationEmailMongo: NotificationEmailMongo = NotificationEmailMongo("someAddress", DateTime.now(), None)
    val testEori = "EoriNumber"
    val mockEmailRepository: EmailRepository = mock[EmailRepository]
    val mockSub22Connector: Sub22Connector = mock[Sub22Connector]
    val mockUndeliverableJobService: UndeliverableJobService = mock[UndeliverableJobService]
    val app: Application = application.overrides(
      inject.bind[EmailRepository].toInstance(mockEmailRepository),
      inject.bind[Sub22Connector].toInstance(mockSub22Connector),
      inject.bind[UndeliverableJobService].toInstance(mockUndeliverableJobService)
    ).build()
  }
}
