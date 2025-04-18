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

import connectors.Sub22Connector
import models.repositories.{NotificationEmailMongo, UndeliverableInformationMongo}
import models.{NotificationEmail, UndeliverableInformation, UndeliverableInformationEvent}

import java.time.LocalDateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import repositories.EmailRepository
import services.UndeliverableJobService
import utils.SpecBase

import scala.concurrent.Future

class UndeliverableEmailControllerSpec extends SpecBase {

  "makeUndeliverable" should {

    "return 404 if record is not found for the EORI while retrieving the data" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some id",
          "groupId"   -> "someGroupId",
          "timestamp" -> currentDateTimeString,
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> currentDateTimeString,
            "code"         -> 12,
            "reason"       -> "unknown reason",
            "tags"         -> Json.obj("enrolment" -> s"HMRC-CUS-ORG~EORINumber~$testEori", "source" -> "sdds")
          )
        )
      )

      running(app) {
        val result = route(app, request).value

        status(result) mustBe NOT_FOUND

        verify(mockEmailRepository, times(0)).findAndUpdate(any(), any())
      }
    }

    "return 404 if user has not been found in the data-store based on EORI for findAndUpdate" in new Setup {

      when(mockEmailRepository.findAndUpdate(any(), any())).thenReturn(Future.successful(None))
      when(mockEmailRepository.get(any())).thenReturn(Future.successful(Some(notificationEmail)))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some id",
          "groupId"   -> "someGroupId",
          "timestamp" -> currentDateTimeString,
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> currentDateTimeString,
            "code"         -> 12,
            "reason"       -> "unknown reason",
            "tags"         -> Json.obj("enrolment" -> s"HMRC-CUS-ORG~EORINumber~$testEori", "source" -> "sdds")
          )
        )
      )

      running(app) {
        val result = route(app, request).value

        status(result) mustBe NOT_FOUND

        verify(mockEmailRepository, times(1)).findAndUpdate(any(), any())
      }
    }

    "return 400 if the enrolment does not contain 'EORINumber'" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some id",
          "groupId"   -> "someGroupId",
          "timestamp" -> currentDateTimeString,
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "enrolment"    -> s"HMRC-CUS-ORG~INVALID~$testEori",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> currentDateTimeString,
            "code"         -> 12,
            "reason"       -> "unknown reason"
          )
        )
      )

      running(app) {
        val result = route(app, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 400 if the enrolment does not contain 'HMRC-CUS-ORG'" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "enrolment"    -> s"INVALID~EORINumber~$testEori",
          "emailAddress" -> "some@email.com",
          "event"        -> "some event",
          "detected"     -> currentDateTimeString,
          "code"         -> 12,
          "reason"       -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "return 400 if the data provided to the endpoint is invalid" in new Setup {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some id",
          "groupId"   -> "someGroupId",
          "timestamp" -> currentDateTimeString,
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> currentDateTimeString,
            "code"         -> 12,
            "reason"       -> "unknown reason"
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

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(Some(notificationEmail)))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some id",
          "groupId"   -> "someGroupId",
          "timestamp" -> currentDateTimeString,
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> currentDateTimeString,
            "code"         -> 12,
            "reason"       -> "unknown reason",
            "tags"         -> Json.obj(
              "enrolment" -> s"HMRC-CUS-ORG~EORINumber~$testEori",
              "source"    -> "sdds"
            )
          )
        )
      )

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 204 if the update was successful to the database" in new Setup {

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

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(Some(notificationEmail)))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some event",
          "groupId"   -> "someGroupId",
          "timestamp" -> detectedDate.toString(),
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> detectedDate.toString(),
            "code"         -> 12,
            "reason"       -> "unknown reason",
            "tags"         -> Json.obj(
              "enrolment" -> s"HMRC-cus-ORG~EORINUMBER~$testEori",
              "source"    -> "sdds"
            )
          )
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe NO_CONTENT

        verify(mockEmailRepository).findAndUpdate(testEori, expectedRequest)
      }
    }

    "return 204 and perform no update" +
      " if email in the request body is different from the email stored in the database" in new Setup {

        when(mockEmailRepository.get(any()))
          .thenReturn(Future.successful(Some(notificationEmail)))

        val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
          Json.obj(
            "subject"   -> "some subject",
            "eventId"   -> "some event",
            "groupId"   -> "someGroupId",
            "timestamp" -> detectedDate.toString,
            "event"     -> Json.obj(
              "id"           -> "some-id",
              "emailAddress" -> "abc@email.com",
              "event"        -> "some event",
              "detected"     -> detectedDate.toString,
              "code"         -> 12,
              "reason"       -> "unknown reason",
              "tags"         -> Json.obj(
                "enrolment" -> s"HMRC-cus-ORG~EORINUMBER~$testEori",
                "source"    -> "sdds"
              )
            )
          )
        )

        running(app) {
          val result = route(app, request).value
          status(result) mustBe NO_CONTENT

          verify(mockEmailRepository, times(0)).findAndUpdate(any(), any())
        }
      }

    "return 204 if the update failed to SUB22" in new Setup {

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

      when(mockEmailRepository.get(any()))
        .thenReturn(Future.successful(Some(notificationEmail)))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(
        Json.obj(
          "subject"   -> "some subject",
          "eventId"   -> "some event",
          "groupId"   -> "someGroupId",
          "timestamp" -> detectedDate.toString,
          "event"     -> Json.obj(
            "id"           -> "some-id",
            "emailAddress" -> "some@email.com",
            "event"        -> "some event",
            "detected"     -> detectedDate.toString,
            "code"         -> 12,
            "reason"       -> "unknown reason",
            "tags"         -> Json.obj(
              "enrolment" -> s"HMRC-cus-ORG~EORINUMBER~$testEori",
              "source"    -> "sdds"
            )
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
    val notificationEmailMongo: NotificationEmailMongo =
      NotificationEmailMongo("someAddress", LocalDateTime.now(), None)
    val testEori                                       = "EoriNumber"
    val currentDateTimeString: String                  = LocalDateTime.now().toString
    val code                                           = 12
    val detectedDate: LocalDateTime                    = LocalDateTime.now()

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      detectedDate.toString,
      Some(code),
      Some("unknown reason"),
      s"HMRC-cus-ORG~EORINUMBER~$testEori",
      Some("sdds")
    )

    val undeliverableInfo: UndeliverableInformation =
      UndeliverableInformation(
        "some subject",
        "some event",
        "someGroupId",
        detectedDate,
        undeliverableInformationEvent
      )

    val notificationEmail: NotificationEmail =
      NotificationEmail("some@email.com", LocalDateTime.now(), Some(undeliverableInfo))

    val postRoute: String = routes.UndeliverableEmailController.makeUndeliverable().url

    val mockEmailRepository: EmailRepository                 = mock[EmailRepository]
    val mockSub22Connector: Sub22Connector                   = mock[Sub22Connector]
    val mockUndeliverableJobService: UndeliverableJobService = mock[UndeliverableJobService]

    val app: Application = application
      .overrides(
        inject.bind[EmailRepository].toInstance(mockEmailRepository),
        inject.bind[Sub22Connector].toInstance(mockSub22Connector),
        inject.bind[UndeliverableJobService].toInstance(mockUndeliverableJobService)
      )
      .build()
  }
}
