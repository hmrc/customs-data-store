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

import models.responses._
import models.{UndeliverableInformation, UndeliverableInformationEvent}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, UpstreamErrorResponse}
import utils.SpecBase

import java.net.URL
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class Sub22ConnectorSpec extends SpecBase {

  "return false if a non 200 response returned from SUB22" in new Setup {
    val errorMsg = "some error"
    val statusCode500 = 500
    val reportAs500 = 500

    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)

    when(requestBuilder.execute(any[HttpReads[UpdateVerifiedEmailResponse]], any[ExecutionContext]))
      .thenReturn(Future.failed(UpstreamErrorResponse(errorMsg, statusCode500, reportAs500)))

    when(mockHttpClient.put(any[URL])(any)).thenReturn(requestBuilder)

    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation,
        LocalDateTime.now(),
        attemptsZero))

      result mustBe false
    }
  }

  "return false if a 200 response returned but 'statusText' is returned indicating an error" in new Setup {
    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)

    when(requestBuilder.execute(any[HttpReads[UpdateVerifiedEmailResponse]], any[ExecutionContext]))
      .thenReturn(Future.successful(failedUpdateVerifiedEmailResponse))

    when(mockHttpClient.put(any[URL])(any)).thenReturn(requestBuilder)

    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation, LocalDateTime.now(), attemptsZero))

      result mustBe false
    }
  }

  "return false if unable to extract the EORI from the undeliverableInformation" in new Setup {
    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation.copy(event = undeliverableInformationEvent.copy(enrolment = "invalid")),
        LocalDateTime.now(),
        attemptsZero))

      result mustBe false
    }
  }

  "return true if the request was successful" in new Setup {
    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)

    when(requestBuilder.execute(any[HttpReads[UpdateVerifiedEmailResponse]], any[ExecutionContext]))
      .thenReturn(Future.successful(successfulUpdateVerifiedEmailResponse))

    when(mockHttpClient.put(any[URL])(any)).thenReturn(requestBuilder)

    running(app) {
      val result = await(connector.updateUndeliverable(
        undeliverableInformation,
        LocalDateTime.now(),
        attemptsZero))

      result mustBe true
    }
  }

  trait Setup {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val testEori = "someEori"
    val detectedDate: LocalDateTime = LocalDateTime.now()
    val attemptsZero = 0
    val code = 12

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

    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]

    val app: Application = new GuiceApplicationBuilder().overrides(
      api.inject.bind[HttpClientV2].toInstance(mockHttpClient),
      api.inject.bind[RequestBuilder].toInstance(requestBuilder)
    ).build()

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
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
