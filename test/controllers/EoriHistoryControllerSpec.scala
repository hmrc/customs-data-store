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

package controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import connectors.EoriHistoryConnector
import models.EoriPeriod
import repositories.{FailedToRetrieveHistoricEori, FailedToUpdateHistoricEori, HistoricEoriRepository, HistoricEoriSuccessful}
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class EoriHistoryControllerSpec extends SpecBase {

  "getEoriHistory" should {
    "return historic EORI's and not call SUB21 if the trader data has eori history defined" in new Setup {
      val eoriPeriods = Seq(EoriPeriod("GB12345678912", Some(date), Some(date)))
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))
      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 200
        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "GB12345678912", "validFrom" -> date, "validUntil" -> date)
        ))
      }
    }

    "return historic EORI's and not call SUB21 if the trader data has eori history defined no from date" in new Setup {
      val eoriPeriods = Seq(EoriPeriod("GB12345678912", None, Some(date)))
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(eoriPeriods)))
      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 200
        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "GB12345678912", "validUntil" -> date)
        ))
      }
    }

    "return historic EORI's and call SUB21 if the trader data has no eori history" in new Setup {
      val eoriPeriods = Seq(EoriPeriod("GB12345678912", None, Some(date)))
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))
      when(mockHistoryService.getHistory(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 200
        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "GB12345678912", "validUntil" -> date)
        ))
      }
    }

    "return internal server error if the update to historic eori's failed" in new Setup {
      val eoriPeriods = Seq(EoriPeriod("GB12345678912", Some(date), Some(date)))
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Right(eoriPeriods)))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(FailedToUpdateHistoricEori))
      when(mockHistoryService.getHistory(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }

    "return internal server error if the trader cannot be found after updating the historic eori's" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Left(FailedToRetrieveHistoricEori)), Future.successful(Left(FailedToRetrieveHistoricEori)))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))
      when(mockHistoryService.getHistory(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }
  }

  "updateEoriHistory" should {
    "return 204 if the update to historic EORI was successful" in new Setup {
      when(mockHistoryService.getHistory(any())).thenReturn(Future.successful(Seq.empty))
      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))
        .thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 204
      }
    }

    "return InternalServerError if the update did not succeed the second time" in new Setup {
      when(mockHistoryService.getHistory(any())).thenReturn(Future.successful(Seq.empty))
      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(HistoricEoriSuccessful))
        .thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }

    "return InternalServerError if the update did not succeed the first time" in new Setup {
      when(mockHistoryService.getHistory(any())).thenReturn(Future.successful(Seq.empty))
      when(mockHistoricEoriRepository.set(any()))
        .thenReturn(Future.successful(FailedToUpdateHistoricEori))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }

    "return InternalServerError if an exception was thrown" in new Setup {
      when(mockHistoryService.getHistory(any())).thenReturn(Future.failed(new RuntimeException("failed")))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(HistoricEoriSuccessful))

      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Right(Seq(EoriPeriod("someEori", None, None)))))

      val body: JsObject = Json.obj("eori" -> "someEori")
      val request = FakeRequest(POST, routes.EoriHistoryController.updateEoriHistory().url).withJsonBody(body)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }



  }

  trait Setup {
    val mockHistoricEoriRepository = mock[HistoricEoriRepository]
    val mockHistoryService = mock[EoriHistoryConnector]
    val testEori = "GB32165498778"
    val date = LocalDate.now().toString

    val app = application.overrides(
      inject.bind[HistoricEoriRepository].toInstance(mockHistoricEoriRepository),
      inject.bind[EoriHistoryConnector].toInstance(mockHistoryService)
    ).build()
  }
}
