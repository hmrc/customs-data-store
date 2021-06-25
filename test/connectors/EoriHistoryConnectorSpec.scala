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

package connectors

import config.AppConfig
import models._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient}
import utils.SpecBase

import java.net.URL
import java.time.LocalDate
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}


class EoriHistoryConnectorSpec extends SpecBase {

  protected def generateResponse(eoris: Seq[String]): HistoricEoriResponse = {
    HistoricEoriResponse(
      GetEORIHistoryResponse(
        ResponseCommon("OK", LocalDate.now().toString),
        ResponseDetail(generateEoriHistory(eoris))
      )
    )
  }

  def generateEoriHistory(allEoris: Seq[String]): Seq[EORIHistory] = {
    val dateCalculator = (years: Int) => "19XX-03-20T19:30:51Z".replaceAll("XX", (85 + allEoris.size - years).toString)

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

  class ETMPScenario() {

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val implicitHeaderCarrier: HeaderCarrier = HeaderCarrier(
      authorization = Option(Authorization("myAwesomeCrypto")),
      otherHeaders = List(("X-very-important", "foo"))
    )

    val someEori = "GB553011111009"
  }

  "EoriHistoryConnector" should {
    "hit the expected URL" in new ETMPScenario {

      val mockHttp = mock[HttpClient]
      val actualURL: ArgumentCaptor[URL] = ArgumentCaptor.forClass(classOf[URL])
      when(mockHttp.GET[HistoricEoriResponse](actualURL.capture(), any[Seq[(String,String)]])(any(), any(), any()))
        .thenReturn(Future.successful(generateResponse(List(someEori))))
      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryConnector]
      val appConfig = app.injector.instanceOf[AppConfig]

      running(app) {
        await(service.getHistory(someEori))
        actualURL.getValue.toString mustBe appConfig.sub21EORIHistoryEndpoint + someEori
      }

    }
  }

  "EoriHistoryConnector" should {
    "return a list of EoriPeriod entries" in new ETMPScenario {
      val jsonResponse =
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
           |          "EORI": "GB550011111009",
           |          "validFrom": "2009-05-16",
           |          "validTo": "2019-07-23"
           |        },
           |        {
           |          "EORI": "GB551011111009",
           |          "validFrom": "2019-07-24",
           |          "validTo": "2019-07-23"
           |        },
           |        {
           |          "EORI": "GB552011111009",
           |          "validFrom": "2019-07-24",
           |          "validTo": "2019-07-23"
           |        }
           |      ]
           |    }
           |  }
           |}""".stripMargin

      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[HistoricEoriResponse](any[URL], any[Seq[(String,String)]])(any(), any(), any()))
        .thenReturn(Future.successful(Json.parse(jsonResponse).as[HistoricEoriResponse]))
      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryConnector]

      running(app) {

        val response = await(service.getHistory(someEori))

        response mustBe List(
          EoriPeriod("GB553011111009", Some("2019-07-24"), None),
          EoriPeriod("GB550011111009", Some("2009-05-16"), Some("2019-07-23")),
          EoriPeriod("GB551011111009", Some("2019-07-24"), Some("2019-07-23")),
          EoriPeriod("GB552011111009", Some("2019-07-24"), Some("2019-07-23"))
        )
      }
    }
  }
}
