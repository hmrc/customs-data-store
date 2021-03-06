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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{EmailRepository, FailedToRetrieveEmail, SuccessfulEmail}
import utils.SpecBase

import scala.concurrent.Future

class UndeliverableEmailControllerSpec extends SpecBase {

  "makeUndeliverable" should {
    "return 404 if user has not been found in the data-store based on EORI" in new Setup {
      when(mockEmailRepository.update(any())).thenReturn(Future.successful(FailedToRetrieveEmail))
      val request = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "enrolmentIdentifier" -> "EORINumber",
          "enrolmentValue" -> testEori,
          "emailAddress" -> "some@email.com",
          "event" -> "some event",
          "detected" -> DateTime.now().toString(),
          "code" -> 12,
          "reason" -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 404
      }
    }

    "return 400 if the enrolment value is not 'EORINumber'" in new Setup {
      val request = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "enrolmentIdentifier" -> "Invalid",
          "enrolmentValue" -> testEori,
          "emailAddress" -> "some@email.com",
          "event" -> "some event",
          "detected" -> DateTime.now().toString(),
          "code" -> 12,
          "reason" -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 400
      }
    }

    "return 400 if the data provided to the endpoint is invalid" in new Setup {
      val request = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "enrolmentValue" -> testEori,
          "emailAddress" -> "some@email.com",
          "event" -> "some event",
          "detected" -> DateTime.now().toString(),
          "code" -> 12,
          "reason" -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 400
      }
    }

    "return 500 if the update to data to the database failed to write" in new Setup {
      when(mockEmailRepository.update(any())).thenReturn(
        Future.failed(new RuntimeException("something went wrong"))
      )

      val request = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "enrolmentIdentifier" -> "EORINumber",
          "enrolmentValue" -> testEori,
          "emailAddress" -> "some@email.com",
          "event" -> "some event",
          "detected" -> DateTime.now().toString(),
          "code" -> 12,
          "reason" -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }

    "return 204 if the update was successful to the database" in new Setup {
      when(mockEmailRepository.update(any())).thenReturn(
        Future.successful(SuccessfulEmail)
      )

      val request = FakeRequest(POST, routes.UndeliverableEmailController.makeUndeliverable().url).withJsonBody(
        Json.obj(
          "enrolmentIdentifier" -> "EORINumber",
          "enrolmentValue" -> testEori,
          "emailAddress" -> "some@email.com",
          "event" -> "some event",
          "detected" -> DateTime.now().toString(),
          "code" -> 12,
          "reason" -> "unknown reason"
        )
      )

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 204
      }
    }
  }

  trait Setup {
    val testEori = "EoriNumber"
    val mockEmailRepository = mock[EmailRepository]
    val app = application.overrides(
      inject.bind[EmailRepository].toInstance(mockEmailRepository)
    ).build()
  }
}
