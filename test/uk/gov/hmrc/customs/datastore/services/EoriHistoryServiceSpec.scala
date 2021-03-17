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

package uk.gov.hmrc.customs.datastore.services

import java.time.LocalDate
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.datastore.config.AppConfig
import uk.gov.hmrc.customs.datastore.domain._
import uk.gov.hmrc.customs.datastore.domain.onwire._
import uk.gov.hmrc.customs.datastore.utils.SpecBase
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.util.Failure


class EoriHistoryServiceSpec extends SpecBase {

  protected def generateResponse(eoris: Seq[Eori]): HistoricEoriResponse = {
    HistoricEoriResponse(
      GetEORIHistoryResponse(
        ResponseCommon("OK", LocalDate.now().toString),
        ResponseDetail(generateEoriHistory(eoris))
      )
    )
  }

  def generateEoriHistory(allEoris: Seq[Eori]): Seq[EORIHistory] = {
    val dateCalculator = (years: Int) => "19XX-03-20T19:30:51Z".replaceAll("XX", (85 + allEoris.size - years).toString)

    @tailrec
    def calcHistory(eoris: Seq[Eori], histories: Seq[EORIHistory]): Seq[EORIHistory] = {
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

  "EoriHistoryService" should {
    "hit the expected URL" in new ETMPScenario {

      val mockHttp = mock[HttpClient]
      val actualURL: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      when(mockHttp.GET[HttpResponse](actualURL.capture())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(generateResponse(List(someEori))),Map.empty[String, Seq[String]])))

      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryService]
      val appConfig = app.injector.instanceOf[AppConfig]

      running(app) {
        await(service.getHistory(someEori))
        actualURL.getValue mustBe appConfig.eoriHistoryUrl + someEori
      }

    }
  }

  "EoriHistoryService" should {
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
           |          "validUntil": "2019-07-23"
           |        },
           |        {
           |          "EORI": "GB551011111009",
           |          "validFrom": "2019-07-24",
           |          "validUntil": "2019-07-23"
           |        },
           |        {
           |          "EORI": "GB552011111009",
           |          "validFrom": "2019-07-24",
           |          "validUntil": "2019-07-23"
           |        }
           |      ]
           |    }
           |  }
           |}""".stripMargin

      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, jsonResponse)))

      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryService]

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

  "EoriHistoryService" should {
    "propagate the HeaderCarrier through to the HTTP request, overwriting the Auth header" in new ETMPScenario {
      val mockHttp = mock[HttpClient]
      val actualHeaderCarrier: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])

      when(mockHttp.GET[HttpResponse](any())(any(), actualHeaderCarrier.capture(), any()))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(generateResponse(List(someEori))).toString())))

      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryService]

      running(app) {

        await(service.getHistory(someEori))

        actualHeaderCarrier.getValue.authorization mustBe Some(Authorization("Bearer secret-token"))
      }
    }
  }

  "EoriHistoryService" should {
    "log an error about exceptions" in new ETMPScenario {
      val errorResponse =
        """<html>
          |<head><title>Error Error 403 Forbidden/Blocked - MDG-WAF 15671#15671: *1445924/Blocked - MDG-WAF:: Empty Uri</title></head>
          |<body bgcolor="white">
          |<center><h1>403 Forbidden</h1></center>
          |<hr><center>nginx</center>
          |</body>
          |</html>""".stripMargin

      val mockHttp = mock[HttpClient]

      val httpResponse = HttpResponse(403, errorResponse, Map.empty[String, Seq[String]])
      when(mockHttp.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(httpResponse))

      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryService]

      running(app) {
        val response = Await.ready(service.getHistory(someEori), 2 seconds)
        response.onComplete {
          case Failure(ex:UpstreamErrorResponse) => ex.statusCode mustBe 403
          case _ =>
        }
      }
    }

    "recover when Throwable occurs" in new ETMPScenario {
      val mockHttp = mock[HttpClient]

      when(mockHttp.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.failed(new Throwable("Boom")))

      private val app: Application = new GuiceApplicationBuilder().overrides(
        api.inject.bind[HttpClient].toInstance(mockHttp)
      ).build()

      val service = app.injector.instanceOf[EoriHistoryService]

      running(app) {
        val response = Await.ready(service.getHistory(someEori), 2 seconds)
        response.onComplete {
          case Failure(ex:UpstreamErrorResponse) => ex.statusCode mustBe 400
          case _ =>
        }
      }
    }
  }
}
