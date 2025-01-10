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
import models.{AddressInformation, CompanyInformation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import repositories.CompanyInformationRepository
import utils.TestData.TEST_EORI_VALUE
import utils.{MockAuthConnector, SpecBase}

import scala.concurrent.Future

class CompanyInformationControllerSpec extends SpecBase with MockAuthConnector {

  "getCompanyInformation" should {

    "return company information if stored in the database" in new Setup {
      when(mockCompanyInformationRepository.get(any()))
        .thenReturn(Future.successful(Some(companyInformation)))

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.getCompanyInformation(TEST_EORI_VALUE).url)

        val result = route(app, request).value

        contentAsJson(result).as[CompanyInformation] mustBe companyInformation
      }
    }

    "return not found if no information found for user" in new Setup {
      when(mockCompanyInformationRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getCompanyInformation(any()))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.getCompanyInformation(TEST_EORI_VALUE).url)

        val result = route(app, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "return company information and store in the database when no existing data in the database" in new Setup {
      when(mockCompanyInformationRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getCompanyInformation(any()))
        .thenReturn(Future.successful(Some(companyInformation)))

      when(mockCompanyInformationRepository.set(TEST_EORI_VALUE, companyInformation)).thenReturn(Future.unit)

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.getCompanyInformation(TEST_EORI_VALUE).url)

        val result = route(app, request).value

        contentAsJson(result).as[CompanyInformation] mustBe companyInformation
      }
    }
  }

  "getCompanyInformationV2" should {

    "return company information if stored in the database" in new Setup {
      when(mockCompanyInformationRepository.get(any()))
        .thenReturn(Future.successful(Some(companyInformation)))

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.getCompanyInformationV2.url)

        val result = route(app, request).value

        contentAsJson(result).as[CompanyInformation] mustBe companyInformation
      }
    }

    "return not found if no information found for user" in new Setup {
      when(mockCompanyInformationRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getCompanyInformation(any()))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.getCompanyInformationV2.url)

        val result = route(app, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "return company information and store in the database when no existing data in the database" in new Setup {
      when(mockCompanyInformationRepository.get(any()))
        .thenReturn(Future.successful(None))

      when(mockSubscriptionInfoConnector.getCompanyInformation(any()))
        .thenReturn(Future.successful(Some(companyInformation)))

      when(mockCompanyInformationRepository.set(TEST_EORI_VALUE, companyInformation)).thenReturn(Future.unit)

      running(app) {
        val request = FakeRequest(GET, routes.CompanyInformationController.getCompanyInformationV2.url)

        val result = route(app, request).value

        contentAsJson(result).as[CompanyInformation] mustBe companyInformation
      }
    }
  }

  trait Setup {
    val addressInformation: AddressInformation =
      AddressInformation("12 Example Street", "Example", Some("AA00 0AA"), "GB")

    val companyInformation: CompanyInformation = CompanyInformation("Example Ltd", "1", addressInformation)

    val mockCompanyInformationRepository: CompanyInformationRepository = mock[CompanyInformationRepository]
    val mockSubscriptionInfoConnector: Sub09Connector                  = mock[Sub09Connector]

    val app: Application = application
      .overrides(
        inject.bind[CompanyInformationRepository].toInstance(mockCompanyInformationRepository),
        inject.bind[Sub09Connector].toInstance(mockSubscriptionInfoConnector),
        inject.bind[CustomAuthConnector].toInstance(mockAuthConnector)
      )
      .build()
  }
}
