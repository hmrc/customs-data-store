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

package uk.gov.hmrc.customs.datastore.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.datastore.domain.EoriPeriod
import uk.gov.hmrc.customs.datastore.repositories.HistoricEoriRepository
import uk.gov.hmrc.customs.datastore.services.EoriHistoryService
import uk.gov.hmrc.customs.datastore.utils.SpecBase

import java.time.LocalDate
import scala.concurrent.Future

class EoriHistoryControllerSpec extends SpecBase {

  "getEoriHistory" should {
    "return historic EORI's and not call SUB21 if the trader data has eori history defined" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(Some(eoriPeriods)))
      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 200
        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "GB12345678912", "validFrom" -> date, "validUntil" -> date)
        ))
      }
    }

    "return historic EORI's and call SUB21 if the trader data has no eori history" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(None), Future.successful(Some(eoriPeriods)))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(true))
      when(mockHistoryService.getHistory(any())(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 200
        contentAsJson(result) mustBe Json.obj("eoriHistory" -> Json.arr(
          Json.obj("eori" -> "GB12345678912", "validFrom" -> date, "validUntil" -> date)
        ))
      }
    }

    "return internal server error if the update to historic eori's failed" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(None), Future.successful(Some(eoriPeriods)))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(false))
      when(mockHistoryService.getHistory(any())(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }

    "return internal server error if the trader cannot be found after updating the historic eori's" in new Setup {
      when(mockHistoricEoriRepository.get(any())).thenReturn(Future.successful(None), Future.successful(None))
      when(mockHistoricEoriRepository.set(any())).thenReturn(Future.successful(true))
      when(mockHistoryService.getHistory(any())(any())).thenReturn(Future.successful(Seq.empty))

      val request = FakeRequest(GET, routes.EoriHistoryController.getEoriHistory(testEori).url)

      running(app) {
        val result = route(app, request).value
        status(result) mustBe 500
      }
    }
  }

  trait Setup {
    val mockHistoricEoriRepository = mock[HistoricEoriRepository]
    val mockHistoryService = mock[EoriHistoryService]
    val testEori = "GB32165498778"
    val date = LocalDate.now().toString

    val eoriPeriods = Seq(EoriPeriod("GB12345678912", Some(date), Some(date)))

    val app = application.overrides(
      inject.bind[HistoricEoriRepository].toInstance(mockHistoricEoriRepository),
      inject.bind[EoriHistoryService].toInstance(mockHistoryService)
    ).build()
  }
}
