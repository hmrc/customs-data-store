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

package connectors

import models.{UndeliverableInformation, UndeliverableInformationEvent}
import models.requests.Sub22Request
import models.responses.{UpdateVerifiedEmailResponse, UpdateVerifiedEmailResponseCommon, UpdateVerifiedEmailResponseCommonDetail}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import utils.SpecBase

import scala.concurrent.Future

class Sub22ConnectorSpec extends SpecBase {
  "return false if a non 200 response returned from SUB22" in new Setup {
    val errorMsg = "some error"
    val statusCode500 = 500
    val reportAs500 = 500

    when(mockHttp.PUT[Sub22Request, UpdateVerifiedEmailResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.failed(UpstreamErrorResponse(errorMsg, statusCode500, reportAs500)))

    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation,
        DateTime.now(),
        attemptsZero))

      result mustBe false
    }
  }

  "return false if a 200 response returned but 'statusText' is returned indicating an error" in new Setup {
    when(mockHttp.PUT[Sub22Request, UpdateVerifiedEmailResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(failedUpdateVerifiedEmailResponse))

    running(app) {
      val result = await(connector.updateUndeliverable(undeliverableInformation, DateTime.now(), attemptsZero))

      result mustBe false
    }
  }

  "return false if unable to extract the EORI from the undeliverableInformation" in new Setup {
    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation.copy(event = undeliverableInformationEvent.copy(enrolment = "invalid")),
        DateTime.now(),
        attemptsZero))

      result mustBe false
    }
  }

  "return true if the request was successful" in new Setup {
    when(mockHttp.PUT[Sub22Request, UpdateVerifiedEmailResponse](any(), any(), any())(any(), any(), any(), any()))
      .thenReturn(Future.successful(successfulUpdateVerifiedEmailResponse))

    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation,
        DateTime.now(),
        attemptsZero))

      result mustBe true
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val testEori = "someEori"
    val detectedDate: DateTime = DateTime.now()
    val attemptsZero = 0

    val successfulUpdateVerifiedEmailResponse: UpdateVerifiedEmailResponse = UpdateVerifiedEmailResponse(
      UpdateVerifiedEmailResponseCommon(
        UpdateVerifiedEmailResponseCommonDetail("OK", None)
      )
    )

    val failedUpdateVerifiedEmailResponse: UpdateVerifiedEmailResponse = UpdateVerifiedEmailResponse(
      UpdateVerifiedEmailResponseCommon(
        UpdateVerifiedEmailResponseCommonDetail("OK", Some("failure"))
      )
    )

    val mockHttp: HttpClient = mock[HttpClient]

    val app: Application = new GuiceApplicationBuilder().overrides(
      api.inject.bind[HttpClient].toInstance(mockHttp)
    ).build()

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$testEori",
      Some("sdds")
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        detectedDate,
        undeliverableInformationEvent
      )

    val connector: Sub22Connector = app.injector.instanceOf[Sub22Connector]
  }

}
