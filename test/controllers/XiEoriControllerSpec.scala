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
import connectors.Sub09Connector
import models.{XiEoriAddressInformation, XiEoriInformation}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import repositories.XiEoriInformationRepository
import utils.TestData.TEST_EORI_VALUE
import utils.{MockAuthConnector, SpecBase}

import scala.concurrent.Future

class XiEoriControllerSpec extends SpecBase with MockAuthConnector {

  "getXiEoriInformation" should {

    "return xi eori information if stored in the database" in new Setup {
      when(mockXiEoriInformationRepository.get(any()))
        .thenReturn(Future.successful(Some(xiEoriInformation)))

      running(app) {
        val request = FakeRequest(GET, getXiEoriInformationRoute)

        val result = route(app, request).value

        contentAsJson(result).as[XiEoriInformation] mustBe xiEoriInformation
      }
    }

    "return not found if no information found for user" in new Setup {
      when(mockXiEoriInformationRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getXiEoriInformation(any()))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = FakeRequest(GET, getXiEoriInformationRoute)

        val result = route(app, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "return company information and store in the database when no existing data in the database" in new Setup {
      when(mockXiEoriInformationRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getXiEoriInformation(any()))
        .thenReturn(Future.successful(Some(xiEoriInformation)))

      when(mockXiEoriInformationRepository.set(eori, xiEoriInformation)).thenReturn(Future.unit)

      running(app) {
        val request = FakeRequest(GET, getXiEoriInformationRoute)

        val result = route(app, request).value

        contentAsJson(result).as[XiEoriInformation] mustBe xiEoriInformation
      }
    }
  }

  "getXiEoriInformationV2" should {

    "return xi eori information for the eori if available in the database" in new Setup {
      when(mockXiEoriInformationRepository.get(eqTo(TEST_EORI_VALUE)))
        .thenReturn(Future.successful(Some(xiEoriInformation)))

      running(app) {
        val request = FakeRequest(GET, getXiEoriInformationV2Route)

        val result = route(app, request).value

        status(result) mustBe OK
        contentAsJson(result).as[XiEoriInformation] mustBe xiEoriInformation
      }
    }

    "return xi eori information for the eori if info is not found in the database but retrieved from Sub09" in new Setup {
      when(mockXiEoriInformationRepository.get(eqTo(TEST_EORI_VALUE)))
        .thenReturn(Future.successful(Some(xiEoriInformation)))

      when(mockSubscriptionInfoConnector.getXiEoriInformation(eqTo(TEST_EORI_VALUE)))
        .thenReturn(Future.successful(Some(xiEoriInformation)))

      running(app) {
        val request = FakeRequest(GET, getXiEoriInformationV2Route)

        val result = route(app, request).value

        contentAsJson(result).as[XiEoriInformation] mustBe xiEoriInformation
      }
    }

    "return NotFound if xi eori information is retrieved neither from the database nor from Sub09" in new Setup {
      when(mockXiEoriInformationRepository.get(eqTo(TEST_EORI_VALUE)))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getXiEoriInformation(eqTo(TEST_EORI_VALUE)))
        .thenReturn(Future.successful(None))

      when(mockXiEoriInformationRepository.set(eqTo(TEST_EORI_VALUE), eqTo(xiEoriInformation)))
        .thenReturn(Future.unit)

      running(app) {
        val request = FakeRequest(GET, getXiEoriInformationV2Route)

        val result = route(app, request).value

        status(result) mustBe NOT_FOUND
      }
    }
  }

  trait Setup {

    val xiEoriAddressInformation: XiEoriAddressInformation =
      XiEoriAddressInformation("12 Example Street", Some("Example"), Some("GB"), None, Some("AA00 0AA"))

    val xiEoriInformation: XiEoriInformation = XiEoriInformation("XI123456789000", "1", xiEoriAddressInformation)
    val eori: String                         = "testEori"

    val getXiEoriInformationRoute: String   = routes.XiEoriController.getXiEoriInformation(eori).url
    val getXiEoriInformationV2Route: String = routes.XiEoriController.getXiEoriInformationV2().url

    val mockXiEoriInformationRepository: XiEoriInformationRepository = mock[XiEoriInformationRepository]
    val mockSubscriptionInfoConnector: Sub09Connector                = mock[Sub09Connector]

    val app: Application = application
      .overrides(
        inject.bind[XiEoriInformationRepository].toInstance(mockXiEoriInformationRepository),
        inject.bind[Sub09Connector].toInstance(mockSubscriptionInfoConnector),
        inject.bind[CustomAuthConnector].toInstance(mockAuthConnector)
      )
      .build()
  }
}
