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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.typesafe.config.ConfigFactory
import config.AppConfig
import models.responses.*
import models.{UndeliverableInformation, UndeliverableInformationEvent}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{SpecBase, WireMockSupportProvider}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class Sub22ConnectorSpec extends SpecBase with WireMockSupportProvider {

  "return false if a non 200 response returned from SUB22" in new Setup {

    wireMockServer.stubFor(
      get(urlPathMatching(sub22Url))
        .withHeader(AUTHORIZATION, equalTo(appConfig.sub09BearerToken))
        .willReturn(
          aResponse()
            .withStatus(SERVICE_UNAVAILABLE)
            .withBody("""{"error": "Service Unavailable"}""")
        )
    )

    val result = await(connector.updateUndeliverable(undeliverableInformation, LocalDateTime.now(), attemptsZero))
    result mustBe false
    verifyEndPointUrlHitWithPut(sub22Url)
  }

  "return false if a 200 response returned but 'statusText' is returned indicating an error" in new Setup {

    val failedUpdate: String = Json.toJson(failedUpdateVerifiedEmailResponse).toString

    wireMockServer.stubFor(
      put(urlPathMatching(sub22Url))
        .willReturn(ok(failedUpdate))
    )

    val result: Boolean =
      connector.updateUndeliverable(undeliverableInformation, LocalDateTime.now(), attemptsZero).futureValue

    result mustBe false
    verifyEndPointUrlHitWithPut(sub22Url)

  }

  "return false if unable to extract the EORI from the undeliverableInformation" in new Setup {

    val failedUpdate: String = Json.toJson(failedUpdateVerifiedEmailResponse).toString

    wireMockServer.stubFor(
      put(urlPathMatching(sub22Url))
        .willReturn(ok(failedUpdate))
    )

    val result: Boolean =
      connector.updateUndeliverable(undeliverableInformation, LocalDateTime.now(), attemptsZero).futureValue

    result mustBe false
    verifyEndPointUrlHitWithPut(sub22Url)
  }

  "return true if the request was successful" in new Setup {

    val updateVerified: String = Json.toJson(successfulUpdateVerifiedEmailResponse).toString

    wireMockServer.stubFor(
      put(urlPathMatching(sub22Url))
        .willReturn(ok(updateVerified))
    )

    val result: Boolean =
      connector.updateUndeliverable(undeliverableInformation, LocalDateTime.now(), attemptsZero).futureValue

    result mustBe true
    verifyEndPointUrlHitWithPut(sub22Url)
  }

  override def config: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |      secure-messaging-frontend {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |    }
         |    sub22 {
         |      host = $wireMockHost
         |      port = $wireMockPort
         |      bearer-token = "secret-token"
         |      updateVerifiedEmailEndpoint = "customs-financials-hods-stub/subscriptions/updateverifiedemail/v1"
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  trait Setup {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    implicit val hc: HeaderCarrier                     = HeaderCarrier()

    val testEori                    = "someEori"
    val detectedDate: LocalDateTime = LocalDateTime.now()
    val attemptsZero                = 0
    val code                        = 12

    val sub22Url: String = "/customs-financials-hods-stub/subscriptions/updateverifiedemail/v1"

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

    val app: Application = application
      .configure(config)
      .build()

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
    val appConfig: AppConfig      = app.injector.instanceOf[AppConfig]
  }
}
