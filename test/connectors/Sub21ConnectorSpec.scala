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
import models.*
import models.responses.{GetEORIHistoryResponse, ResponseCommon, ResponseDetail}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpErrorFunctions.notFoundMessage
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.{SpecBase, WireMockSupportProvider}
import com.typesafe.config.ConfigFactory
import play.api.{Application, Configuration}
import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlPathMatching}
import org.scalatest.concurrent.ScalaFutures._

import java.net.URL
import java.time.LocalDate
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class Sub21ConnectorSpec extends SpecBase with WireMockSupportProvider  {

  "EoriHistoryConnector" should {
    "hit the expected URL" in new Setup {
      
      val url: String = "/customs-financials-hods-stub/eorihistory/testEori"
      val emailUnVerifiedResJson01: String = Json.toJson(generateResponse(List(someEori))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(url))
          .willReturn(ok(emailUnVerifiedResJson01))
      )

      val result: Seq[models.EoriPeriod] = connector.getEoriHistory(someEori).futureValue
      verifyEndPointUrlHit(url)
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

      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[HistoricEoriResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(Json.parse(jsonResponse).as[HistoricEoriResponse]))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {

        val response = await(connector.getEoriHistory(someEori))

        response mustBe List(
          EoriPeriod("testEori", Some("2019-07-24"), None),
          EoriPeriod("historicEori1", Some("2009-05-16"), Some("2019-07-23")),
          EoriPeriod("historicEori2", Some("2019-07-24"), Some("2019-07-23")),
          EoriPeriod("historicEori3", Some("2019-07-24"), Some("2019-07-23"))
        )
      }
    }

    "recoverWith Not Found" in new Setup {

      val compare: Future[Nothing] =
        Future.failed(UpstreamErrorResponse(notFoundMessage("GET", actualURL.toString, "error1"), NOT_FOUND))

      when(requestBuilder.execute(any[HttpReads[HistoricEoriResponse]], any[ExecutionContext])).thenReturn(compare)

      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)
      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        assertThrows[NotFoundException] {
          await(connector.getEoriHistory(someEori))
        }
      }
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
         |    sub21 {
         |      host = $wireMockHost
         |      port = $wireMockPort
         |      bearer-token = "secret-token"
         |      companyInformationEndpoint = "customs-financials-hods-stub/eorihistory"
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    implicit val implicitHeaderCarrier: HeaderCarrier = HeaderCarrier(
      authorization = Option(Authorization("myAwesomeCrypto")),
      otherHeaders = List(("X-very-important", "foo"))
    )

    val actualURL: ArgumentCaptor[URL] = ArgumentCaptor.forClass(classOf[URL])

    val someEori                       = "testEori"
    val mockHttpClient: HttpClientV2   = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]

    val app: Application = application
      .configure(config)
      .build()

    val connector: Sub21Connector = app.injector.instanceOf[Sub21Connector]
    val appConfig: AppConfig      = app.injector.instanceOf[AppConfig]
  }

  protected def generateResponse(eoris: Seq[String]): HistoricEoriResponse =
    HistoricEoriResponse(
      GetEORIHistoryResponse(ResponseCommon("OK", LocalDate.now().toString), ResponseDetail(generateEoriHistory(eoris)))
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
}
