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

import config.AppConfig
import config.Headers.{DATE, X_CORRELATION_ID, X_FORWARDED_HOST}
import models.*
import models.responses.{GetEORIHistoryResponse, ResponseCommon, ResponseDetail}
import org.mockito.ArgumentCaptor
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.Json
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpErrorFunctions.notFoundMessage
import utils.{SpecBase, WireMockSupportProvider}
import play.api.http.HeaderNames.AUTHORIZATION
import com.typesafe.config.ConfigFactory
import play.api.{Application, Configuration}
import com.github.tomakehurst.wiremock.client.WireMock.{
  aResponse, absent, equalTo, get, getRequestedFor, matching, ok, urlPathEqualTo, urlPathMatching, verify
}
import org.scalatest.concurrent.ScalaFutures.*

import java.net.URL
import java.time.LocalDate
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class Sub24ConnectorSpec extends SpecBase with WireMockSupportProvider {

  "Sub24Connector" should {
    "call SUB24 with the EORI and required headers for GB-only history" in new Setup {

      val response: String = Json.toJson(generateResponse(List(someEori))).toString

      wireMockServer.stubFor(
        get(urlPathEqualTo(sub24Url))
          .withQueryParam("eori", equalTo(someEori))
          .willReturn(ok(response))
      )

      await(connector.getEoriHistory(someEori))

      verify(
        getRequestedFor(urlPathEqualTo(sub24Url))
          .withQueryParam("eori", equalTo(someEori))
          .withQueryParam("association", absent())
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub24BearerToken))
          .withHeader(DATE, matching(".+"))
          .withHeader(X_CORRELATION_ID, matching("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
      )
    }

    "call SUB24 with the EORI, association and required headers for GB and XI history" in new Setup {

      val response: String = Json.toJson(generateResponse(List(someEori))).toString

      wireMockServer.stubFor(
        get(urlPathEqualTo(sub24Url))
          .withQueryParam("eori", equalTo(someEori))
          .withQueryParam("association", equalTo("1"))
          .willReturn(ok(response))
      )

      await(connector.getEoriHistory(someEori, false))
      verify(
        getRequestedFor(urlPathEqualTo(sub24Url))
          .withQueryParam("eori", equalTo(someEori))
          .withQueryParam("association", equalTo("1"))
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub24BearerToken))
          .withHeader(DATE, matching(".+"))
          .withHeader(X_CORRELATION_ID, matching("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
      )
    }

    "return a list of EoriPeriod entries" in new Setup {
      val jsonResponse: String =
        s"""{
           |  "getEORIHistoryResponse": {
           |    "responseCommon": {
           |      "status": "OK",
           |      "processingDate": "2019-07-26T10:21:13Z"
           |    },
           |    "responseDetail": {
           |      "EORIHistory": [
           |        {
           |          "EORI": "$someEori",
           |          "validFrom": "2019-07-24"
           |        },
           |        {
           |          "EORI": "historicEori1",
           |          "validFrom": "2009-05-16",
           |          "validTo": "2019-07-23"
           |        },
           |        {
           |          "EORI": "historicEori2",
           |          "validFrom": "2019-07-24",
           |          "validTo": "2019-07-23"
           |        },
           |        {
           |          "EORI": "historicEori3",
           |          "validFrom": "2019-07-24",
           |          "validTo": "2019-07-23"
           |        }
           |      ]
           |    }
           |  }
           |}""".stripMargin

      val response: String = Json.toJson(Json.parse(jsonResponse).as[HistoricEoriResponse]).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub24Url))
          .willReturn(ok(response))
      )

      val result: Seq[models.EoriPeriod] = connector.getEoriHistory(someEori).futureValue

      result mustBe List(
        EoriPeriod("testEori", Some("2019-07-24"), None),
        EoriPeriod("historicEori1", Some("2009-05-16"), Some("2019-07-23")),
        EoriPeriod("historicEori2", Some("2019-07-24"), Some("2019-07-23")),
        EoriPeriod("historicEori3", Some("2019-07-24"), Some("2019-07-23"))
      )

      verifyEndPointUrlHit(sub24Url)
    }

    "recoverWith Not Found" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub24Url))
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub24BearerToken))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(notFoundMessage("GET", actualURL.toString, "error1"))
          )
      )
      val result: Seq[EoriPeriod] = await(connector.getEoriHistory(someEori))
      result mustBe empty
    }

    "propagate unexpected upstream errors" in new Setup {
      wireMockServer.stubFor(
        get(urlPathEqualTo(sub24Url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val error: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(connector.getEoriHistory(someEori))
      }

      error.statusCode mustBe INTERNAL_SERVER_ERROR
    }
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
         |    sub24 {
         |      host = $wireMockHost
         |      port = $wireMockPort
         |      bearer-token = "secret-token"
         |      historicEoriEndpoint = "customs-financials-hods-stub/gbxieorihistory/"
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  def generateEoriHistory(allEoris: Seq[String]): Seq[EORIHistory] = {
    val dateCalculator =
      (years: Int) => "19XX-03-20T19:30:51Z".replaceAll("XX", (85 + allEoris.size - years).toString)

    @tailrec
    def calcHistory(eoris: Seq[String], histories: Seq[EORIHistory]): Seq[EORIHistory] = {
      val eori = eoris.head

      eoris.size match {
        case 1 =>
          EORIHistory(eori, Some(dateCalculator(eoris.size)), None) +: histories
        case _ =>
          val current = EORIHistory(eori, Some(dateCalculator(eoris.size)), Some(dateCalculator(eoris.size - 1)))
          calcHistory(eoris.tail, current +: histories)
      }
    }

    calcHistory(allEoris, Seq.empty[EORIHistory])
  }

  protected def generateResponse(eoris: Seq[String]): HistoricEoriResponse =
    HistoricEoriResponse(
      GetEORIHistoryResponse(ResponseCommon("OK", LocalDate.now().toString), ResponseDetail(generateEoriHistory(eoris)))
    )

  trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val hc: HeaderCarrier    = HeaderCarrier()

    val sub24Url: String = "/customs-financials-hods-stub/gbxieorihistory/"
    val someEori: String = "testEori"

    implicit val implicitHeaderCarrier: HeaderCarrier = HeaderCarrier(
      authorization = Option(Authorization("myAwesomeCrypto")),
      otherHeaders = List(("X-very-important", "foo"))
    )

    val actualURL: ArgumentCaptor[URL] = ArgumentCaptor.forClass(classOf[URL])

    val app: Application = application
      .configure(config)
      .build()

    val connector: Sub24Connector = app.injector.instanceOf[Sub24Connector]
    val appConfig: AppConfig      = app.injector.instanceOf[AppConfig]
  }
}
