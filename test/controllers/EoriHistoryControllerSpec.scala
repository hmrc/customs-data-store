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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.{Application, inject}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import connectors.Sub21Connector
import models.EoriPeriod
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import repositories.{
  FailedToRetrieveHistoricEori, FailedToUpdateHistoricEori, HistoricEoriRepository, HistoricEoriSuccessful
}
import utils.{MockAuthConnector, SpecBase}
import utils.TestData.TEST_EORI_VALUE

import java.time.LocalDate
import scala.concurrent.Future

class EoriHistoryControllerSpec extends SpecBase with MockAuthConnector {

  "getEoriHistory" should {
    "return 200 OK and skip caching if sub21 returns no EORI history (empty Seq)" in new Setup {
      private val testEori = "testEori"
      private val getRoute: String = routes.EoriHistoryController.getEoriHistory(testEori).url

      when(mockHistoricEoriRepository.get(eqTo(testEori)))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      running(app) {
        val request = FakeRequest(GET, getRoute)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr()
        )
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined" in new Setup {
      private val testEori         = "testEori"
      private val getRoute: String = routes.EoriHistoryController.getEoriHistory(testEori).url

      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(eqTo(testEori))).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request = FakeRequest(GET, getRoute)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validFrom" -> date, "validUntil" -> date)
          )
        )
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined no from date" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request = FakeRequest(GET, getRoute)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validUntil" -> date)
          )
        )
      }
    }

    "return historic EORI's and call SUB21 if the trader data has no eori history" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getRoute)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validUntil" -> date)
          )
        )
      }
    }

    "return internal server error if the update to historic eori's failed" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getRoute)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return internal server error if the trader cannot be found after updating the historic eori's" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(
          Future.successful(Left(FailedToRetrieveHistoricEori)),
          Future.successful(Left(FailedToRetrieveHistoricEori))
        )

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getRoute)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "getEoriHistoryV2" should {
    "return 200 OK and skip caching is sub21 returns no EORI history (empty Seq)" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      running(app) {
        val request = FakeRequest(GET, getRouteV2)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr()
        )
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request = FakeRequest(GET, getRouteV2)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validFrom" -> date, "validUntil" -> date)
          )
        )
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined no from date" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request = FakeRequest(GET, getRouteV2)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validUntil" -> date)
          )
        )
      }
    }

    "return historic EORI's and call SUB21 if the trader data has no eori history" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(
          Future.successful(Left(FailedToRetrieveHistoricEori)),
          Future.successful(Right(eoriPeriods))
        )

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getRouteV2)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validUntil" -> date)
          )
        )
      }
    }

    "return internal server error if the update to historic eori's failed" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getRouteV2)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return internal server error if the trader cannot be found after updating the historic eori's" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(
          Future.successful(Left(FailedToRetrieveHistoricEori)),
          Future.successful(Left(FailedToRetrieveHistoricEori))
        )

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any()))
        .thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getRouteV2)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "retrieveEoriHistoryThirdParty" should {
    "return 200 OK and skip caching if sub21 returns no EORI history (empty Seq)" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      running(app) {
        val request: FakeRequest[AnyContentAsJson] =
          FakeRequest(POST, getRouteThirdParty).withJsonBody(Json.obj("eori" -> TEST_EORI_VALUE))

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr()
        )
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request: FakeRequest[AnyContentAsJson] =
          FakeRequest(POST, getRouteThirdParty).withJsonBody(Json.obj("eori" -> TEST_EORI_VALUE))

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validFrom" -> date, "validUntil" -> date)
          )
        )
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined no from date" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request: FakeRequest[AnyContentAsJson] =
          FakeRequest(POST, getRouteThirdParty).withJsonBody(Json.obj("eori" -> TEST_EORI_VALUE))

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validUntil" -> date)
          )
        )
      }
    }

    "return historic EORI's and call SUB21 if the trader data has no eori history" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(POST, getRouteThirdParty).withJsonBody(Json.obj("eori" -> TEST_EORI_VALUE))

      running(app) {
        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj(
          "eoriHistory" -> Json.arr(
            Json.obj("eori" -> "testEori", "validUntil" -> date)
          )
        )
      }
    }

    "return internal server error if the update to historic eori's failed" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(POST, getRouteThirdParty).withJsonBody(Json.obj("eori" -> TEST_EORI_VALUE))

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return internal server error if the trader cannot be found after updating the historic eori's" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(
          Future.successful(Left(FailedToRetrieveHistoricEori)),
          Future.successful(Left(FailedToRetrieveHistoricEori))
        )

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(eoriPeriods))

      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(POST, getRouteThirdParty).withJsonBody(Json.obj("eori" -> TEST_EORI_VALUE))

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "updateEoriHistory" should {
    "return 204 if the update to historic EORI was successful" in new Setup {
      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))
        .thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject                         = Json.obj("eori" -> "someEori")
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe NO_CONTENT
      }
    }

    "return InternalServerError if the update did not succeed the second time" in new Setup {
      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))
        .thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject                         = Json.obj("eori" -> "someEori")
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return InternalServerError if the update did not succeed the first time" in new Setup {
      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject                         = Json.obj("eori" -> "someEori")
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return InternalServerError if error (other than NotFound) occurs while retrieving an EORI" in new Setup {
      when(mockHistoryService.getEoriHistory(any()))
        .thenReturn(Future.failed(new RuntimeException("Error Occurred")))

      val body: JsObject                         = Json.obj("eori" -> eori)
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, postRoute).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  trait Setup {
    val testEori     = "testEori"
    val date: String = LocalDate.now().toString
    val eori         = "test_eori"

    val getRoute: String           = routes.EoriHistoryController.getEoriHistory(testEori).url
    val getRouteV2: String         = routes.EoriHistoryController.getEoriHistoryV2().url
    val getRouteThirdParty: String = routes.EoriHistoryController.retrieveEoriHistoryThirdParty().url
    val postRoute: String          = routes.EoriHistoryController.updateEoriHistory().url

    val mockHistoricEoriRepository: HistoricEoriRepository = mock[HistoricEoriRepository]
    val mockHistoryService: Sub21Connector                 = mock[Sub21Connector]

    val app: Application = application
      .overrides(
        inject.bind[HistoricEoriRepository].toInstance(mockHistoricEoriRepository),
        inject.bind[Sub21Connector].toInstance(mockHistoryService),
        inject.bind[CustomAuthConnector].toInstance(mockAuthConnector)
      )
      .build()
  }
}
