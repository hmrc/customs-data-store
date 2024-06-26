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

import models.{AddressInformation, CompanyInformation, XiEoriAddressInformation, XiEoriInformation}
import models.responses.{MdgSub09CompanyInformationResponse, MdgSub09Response, MdgSub09XiEoriInformationResponse, Sub09Response}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers.running
import utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, ServiceUnavailableException}
import utils.Utils.emptyString

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Sub09ConnectorSpec extends SpecBase {

  "getSubscriberInformation" should {

    "return None when the timestamp is not available" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailNoTimestamp(testEori))))

      running(app) {
        await(service.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "return Some, when the timestamp is available" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailAndTimestamp(testEori))))

      running(app) {
        val result = await(service.getSubscriberInformation(testEori)).value
        result.address mustBe "email@email.com"
      }
    }

    "return None when the email is not available" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.noEmailNoTimestamp(testEori))))

      running(app) {
        await(service.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "propagate ServiceUnavailableException" in new Setup {
      when(mockHttp.GET[MdgSub09Response](any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      running(app) {
        assertThrows[ServiceUnavailableException](await(service.getSubscriberInformation(testEori)))
      }
    }
  }

  "getCompanyInformation" should {
    "return company information from the api" in new Setup {
      when(mockHttp.GET[Option[MdgSub09CompanyInformationResponse]](
        any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(
          Some(mdgCompanyInformationResponse(Sub09Response.withEmailNoTimestamp(testEori))))
        )

      running(app) {
        await(service.getCompanyInformation(testEori)) mustBe Some(companyInformation)
      }
    }

    "return company information noConsent '0' when the field is not present" in new Setup {
      when(mockHttp.GET[Option[MdgSub09CompanyInformationResponse]](
        any[URL],
        any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(
          Some(mdgCompanyInformationResponse(Sub09Response.noConsentToDisclosureOfPersonalData(testEori))))
        )

      running(app) {
        await(service.getCompanyInformation(testEori)) mustBe Some(companyInformationNoConsentFalse)
      }
    }

    "return None on failure" in new Setup {
      when(mockHttp.GET[Option[MdgSub09CompanyInformationResponse]](
        any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      running(app) {
        await(service.getCompanyInformation(testEori)) mustBe None
      }
    }
  }

  "getXiEoriInformation" should {
    "return xi eori information from the api" in new Setup {
      when(mockHttp.GET[Option[MdgSub09XiEoriInformationResponse]](
        any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(
          Some(mdgXiEoriInformationResponse(Sub09Response.withEmailAndTimestamp(testEori))))
        )

      running(app) {
        service.getXiEoriInformation(testEori).map {
          xiInfo => xiInfo mustBe Some(xiEoriInformation)
        }
      }
    }

    "return xi eori information from the api when pbeaddress is empty" in new Setup {
      when(mockHttp.GET[Option[MdgSub09XiEoriInformationResponse]](
        any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.successful(
          Some(mdgXiEoriInformationResponse(Sub09Response.noXiEoriAddressInformation(testEori))))
        )

      running(app) {
        service.getXiEoriInformation(testEori).map {
          xiInfo => xiInfo mustBe Some(xiEoriInformationWithNoAddress)
        }
      }
    }

    "return None on failure" in new Setup {
      when(mockHttp.GET[Option[MdgSub09XiEoriInformationResponse]](
        any[URL], any[Seq[(String, String)]])(any(), any(), any()))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      running(app) {
        service.getXiEoriInformation(testEori).map {
          xiInfo => xiInfo mustBe None
        }
      }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val testEori = "someEori"
    val xiEori = "XI123456789000"
    val companyName = "Example Ltd"
    val consent = "1"

    val address: AddressInformation = AddressInformation("Address Line 1", "City", Some("postCode"), "GB")

    val companyInformation: CompanyInformation = CompanyInformation(companyName, consent, address)

    val companyInformationNoConsentFalse: CompanyInformation = CompanyInformation(companyName, "0", address)

    val xiEoriInformation: XiEoriInformation =
      XiEoriInformation(xiEori, consent,
        XiEoriAddressInformation("Example Rd", Some("Example"), Some("GB"), None, Some("AA00 0AA")))

    val xiEoriInformationWithNoAddress: XiEoriInformation =
      XiEoriInformation(xiEori, consent, XiEoriAddressInformation(emptyString))

    val mockHttp: HttpClient = mock[HttpClient]

    val app: Application = application.overrides(
      bind[HttpClient].toInstance(mockHttp)
    ).build()

    val service: Sub09Connector = app.injector.instanceOf[Sub09Connector]

    def mdgResponse(value: JsValue): MdgSub09Response = MdgSub09Response.sub09Reads.reads(value).get

    def mdgCompanyInformationResponse(value: JsValue): MdgSub09CompanyInformationResponse =
      MdgSub09CompanyInformationResponse.sub09CompanyInformation.reads(value).get

    def mdgXiEoriInformationResponse(value: JsValue): MdgSub09XiEoriInformationResponse =
      MdgSub09XiEoriInformationResponse.sub09XiEoriInformation.reads(value).get
  }
}
