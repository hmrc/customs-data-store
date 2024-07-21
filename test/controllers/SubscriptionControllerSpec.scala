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

import models.{EORI, EmailAddress}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import services.SubscriptionService
import uk.gov.hmrc.http.NotFoundException
import utils.SpecBase
import org.mockito.Mockito.when
import models.responses.{EmailUnverifiedResponse, EmailVerifiedResponse}

import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase {

  "SubscriptionController.get" should {
    "return 200 status code" in new Setup {

      val subscriptionResponse: EmailVerifiedResponse = EmailVerifiedResponse(None)

      when(mockSubscriptionService.getVerifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.successful(subscriptionResponse))

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return 200 status code for unVerified email" in new Setup {
      val subscriptionResponse: EmailUnverifiedResponse = EmailUnverifiedResponse(None)

      when(mockSubscriptionService.getUnverifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.successful(subscriptionResponse))

      running(app) {
        val result = route(app, unVerifiedEmailRequest).value
        status(result) mustBe OK
      }
    }

    "return 200 status code for getEmail" in new Setup {
      val subscriptionResponse: EmailVerifiedResponse = EmailVerifiedResponse(Some(EmailAddress("test@email.com")))

      when(mockSubscriptionService.getEmailAddress(eqTo(traderEORI)))
        .thenReturn(Future.successful(subscriptionResponse))

      running(app) {
        val result = route(app, getEmailAddressrequest).value
        status(result) mustBe OK
      }
    }

    "return 503 for any error" in new Setup {
      when(mockSubscriptionService.getVerifiedEmail(eqTo(traderEORI)))
        .thenReturn(Future.failed(new NotFoundException("ShouldNotReturnThis")))

      running(app) {
        val result = route(app, request).value
        status(result) mustBe SERVICE_UNAVAILABLE
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

    "return 503 for any error in getEmail" in new Setup {
      when(mockSubscriptionService.getEmailAddress(eqTo(traderEORI)))
        .thenReturn(Future.failed(new NotFoundException("ShouldNotReturnThis")))

      running(app) {
        val result = route(app, getEmailAddressrequest).value
        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }
  }

  trait Setup {

    val traderEORI: EORI = EORI("test_eori")

    val request: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", controllers.routes.SubscriptionController.getVerifiedEmail().url)

    val getEmailAddressrequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", controllers.routes.SubscriptionController.getEmail().url)

    val unVerifiedEmailRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", controllers.routes.SubscriptionController.getUnverifiedEmail().url)

    val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[SubscriptionService].toInstance(mockSubscriptionService)
    ).configure(
      "microservice.metrics.enabled" -> false,
      "metrics.enabled" -> false,
      "auditing.enabled" -> false
    ).build()
  }
}
