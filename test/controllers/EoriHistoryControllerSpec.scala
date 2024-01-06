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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.{Application, inject}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import connectors.Sub21Connector
import models.EoriPeriod
import play.api.mvc.AnyContentAsJson
import repositories.{FailedToRetrieveHistoricEori, FailedToUpdateHistoricEori, HistoricEoriRepository, HistoricEoriSuccessful}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class EoriHistoryControllerSpec extends SpecBase {

  "getEoriHistory" should {
    "return historic EORI's and not call SUB21 if the trader data has eori history defined" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "testEori", "validFrom" -> date, "validUntil" -> date)
        ))
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined no from date" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))


      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))

      running(app) {
        val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

        val result = route(app, request).value

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "testEori", "validUntil" -> date)
        ))
      }
    }

    "return historic EORI's and call SUB21 if the trader data has no eori history" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", None, Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "testEori", "validUntil" -> date)
        ))
      }
    }

    "return internal server error if the update to historic eori's failed" in new Setup {
      val eoriPeriods: Seq[EoriPeriod] = Seq(EoriPeriod("testEori", Some(date), Some(date)))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return internal server error if the trader cannot be found after updating the historic eori's" in new Setup {
      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(
          Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Left(FailedToRetrieveHistoricEori))
        )

      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

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

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

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

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

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

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return Not Found if we cannot find an EORI" in new Setup {
      when(mockHistoryService.getEoriHistory(any())).thenReturn(Future.failed(new RuntimeException("Not found")))

      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoricEoriRepository.get(any()))
        .thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "return InternalServerError if error (other than NotFound) occurs while retrieving an EORI" in new Setup {
      when(mockHistoryService.getEoriHistory(any()))
        .thenReturn(Future.failed(new RuntimeException("Error Occurred")))

      val body: JsObject = Json.obj("eori" -> eori)
      val request: FakeRequest[AnyContentAsJson] =
        FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  trait Setup {
    val mockHistoricEoriRepository: HistoricEoriRepository = mock[HistoricEoriRepository]
    val mockHistoryService: Sub21Connector = mock[Sub21Connector]
    val testEori = "testEori"
    val date: String = LocalDate.now().toString
    val eori = "test_eori"

    val app: Application = application.overrides(
      inject.bind[HistoricEoriRepository].toInstance(mockHistoricEoriRepository),
      inject.bind[Sub21Connector].toInstance(mockHistoryService)
    ).build()
  }
}
