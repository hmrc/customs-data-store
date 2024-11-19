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
import config.Platform.{ENROLMENT_IDENTIFIER, ENROLMENT_KEY}
import models.{EORI, EmailAddress, NotificationEmail}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import services.SubscriptionService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.NotFoundException
import utils.SpecBase
import org.mockito.Mockito.when
import models.responses.{EmailUnverifiedResponse, EmailVerifiedResponse}
import repositories.EmailRepository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase {

  "getVerifiedEmail" should {

    "return 200 status code if data is available in cache" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(Some(notificationEmail)))

      val getRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", controllers.routes.SubscriptionController.getVerifiedEmail().url)

      running(app) {
        val result = route(app, getRequest).value
        status(result) mustBe OK
      }
    }

    "return 200 status code if data is unavailable in cache but available in ETMP" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))

      when(mockSubscriptionService.getVerifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.successful(emailVerifiedResponse01))

      val getRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", controllers.routes.SubscriptionController.getVerifiedEmail().url)

      running(app) {
        val result = route(app, getRequest).value
        status(result) mustBe OK
      }
    }

    "return 503 for any error" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))

      when(mockSubscriptionService.getVerifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.failed(new NotFoundException("ShouldNotReturnThis")))

      val getRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", controllers.routes.SubscriptionController.getVerifiedEmail().url)

      running(app) {
        val result = route(app, getRequest).value
        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }
  }

  "getUnverifiedEmail" should {

    "return 200 status code for unVerified email" in new Setup {

      val emailUnverifiedResponse: EmailUnverifiedResponse = EmailUnverifiedResponse(None)

      when(mockSubscriptionService.getUnverifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.successful(emailUnverifiedResponse))

      running(app) {
        val result = route(app, unVerifiedEmailRequest).value
        status(result) mustBe OK
      }
    }

    "return 503 for any error in unverified email" in new Setup {
      when(mockSubscriptionService.getUnverifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.failed(new NotFoundException("ShouldNotReturnThis")))

      running(app) {
        val result = route(app, unVerifiedEmailRequest).value
        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }

  }

  "getEmailAddress" should {

    "return 200 status code if data is available in cache" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(Some(notificationEmail)))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", controllers.routes.SubscriptionController.getEmail().url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return 200 status code if data is unavailable in cache but available in ETMP" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))

      when(mockSubscriptionService.getEmailAddress(eqTo(traderEORI)))
        .thenReturn(Future.successful(emailVerifiedResponse01))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", controllers.routes.SubscriptionController.getEmail().url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return 503 for any error in getEmail" in new Setup {

      when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))

      when(mockSubscriptionService.getEmailAddress(eqTo(traderEORI)))
        .thenReturn(Future.failed(new NotFoundException("ShouldNotReturnThis")))

      val getEmailAddressrequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", controllers.routes.SubscriptionController.getEmail().url)

      running(app) {
        val result = route(app, getEmailAddressrequest).value
        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }

  }

  trait Setup {

    val traderEORI: EORI = EORI("test_eori")
    val email: String = "test@email.com"
    val dateTimeStr: String = "2020-10-05T09:30:47Z"

    val localDateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME)

    val notificationEmail: NotificationEmail = NotificationEmail(email, localDateTime, None)

    val emailVerifiedResponse01: EmailVerifiedResponse = EmailVerifiedResponse(Some(EmailAddress(email)))

    val enrolments: Enrolments = Enrolments(
      Set(Enrolment(ENROLMENT_KEY,
        Seq(EnrolmentIdentifier(ENROLMENT_IDENTIFIER, traderEORI.value)),
        "activated")))

    val unVerifiedEmailRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", controllers.routes.SubscriptionController.getUnverifiedEmail().url)

    val mockAuthConnector: CustomAuthConnector = mock[CustomAuthConnector]
    val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]
    val mockEmailRepository: EmailRepository = mock[EmailRepository]

    when(mockAuthConnector.authorise[Enrolments](any, any)(any, any)).thenReturn(Future.successful(enrolments))

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[CustomAuthConnector].toInstance(mockAuthConnector),
      inject.bind[SubscriptionService].toInstance(mockSubscriptionService),
      inject.bind[EmailRepository].toInstance(mockEmailRepository)
    ).configure(
      "microservice.metrics.enabled" -> false,
      "metrics.enabled" -> false,
      "auditing.enabled" -> false
    ).build()
  }
}
