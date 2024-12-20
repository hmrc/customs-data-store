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

import connectors.Sub09Connector
import models.{XiEoriAddressInformation, XiEoriInformation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.XiEoriInformationRepository
import utils.SpecBase

import scala.concurrent.Future

class XiEoriControllerSpec extends SpecBase {

  "getXiEoriInformation" should {

    "return xi eori information if stored in the database" in new Setup {
      when(mockXiEoriInformationRepository.get(any()))
        .thenReturn(Future.successful(Some(xiEoriInformation)))

      running(app) {
        val request = FakeRequest(GET, getRoute)

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
        val request = FakeRequest(GET, getRoute)

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
        val request = FakeRequest(GET, getRoute)

        val result = route(app, request).value

        contentAsJson(result).as[XiEoriInformation] mustBe xiEoriInformation
      }
    }
  }

  trait Setup {

    val xiEoriAddressInformation: XiEoriAddressInformation =
      XiEoriAddressInformation("12 Example Street", Some("Example"), Some("GB"), None, Some("AA00 0AA"))

    val xiEoriInformation: XiEoriInformation = XiEoriInformation("XI123456789000", "1", xiEoriAddressInformation)
    val eori: String                         = "testEori"

    val getRoute: String = routes.XiEoriController.getXiEoriInformation(eori).url

    val mockXiEoriInformationRepository: XiEoriInformationRepository = mock[XiEoriInformationRepository]
    val mockSubscriptionInfoConnector: Sub09Connector                = mock[Sub09Connector]

    val app: Application = application
      .overrides(
        inject.bind[XiEoriInformationRepository].toInstance(mockXiEoriInformationRepository),
        inject.bind[Sub09Connector].toInstance(mockSubscriptionInfoConnector)
      )
      .build()
  }
}
