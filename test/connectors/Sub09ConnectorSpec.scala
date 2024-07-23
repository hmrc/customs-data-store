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

import models.responses.*
import models.{AddressInformation, CompanyInformation, XiEoriAddressInformation, XiEoriInformation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.JsValue
import play.api.test.Helpers.running
import play.api.{Application, inject}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, ServiceUnavailableException}
import utils.SpecBase
import utils.Utils.emptyString
import utils.TestData.*

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class Sub09ConnectorSpec extends SpecBase {

  "getSubscriberInformation" should {
    "return None when the timestamp is not available" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09Response]], any[ExecutionContext]))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailNoTimestamp(testEori))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        await(connector.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "return Some, when the timestamp is available" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09Response]], any[ExecutionContext]))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.withEmailAndTimestamp(testEori))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val result = await(connector.getSubscriberInformation(testEori)).value
        result.address mustBe "email@email.com"
      }
    }

    "return None when the email is not available" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09Response]], any[ExecutionContext]))
        .thenReturn(Future.successful(mdgResponse(Sub09Response.noEmailNoTimestamp(testEori))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        await(connector.getSubscriberInformation(testEori)) mustBe None
      }
    }

    "propagate ServiceUnavailableException" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09Response]], any[ExecutionContext]))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        assertThrows[ServiceUnavailableException](await(connector.getSubscriberInformation(testEori)))
      }
    }
  }

  "getCompanyInformation" should {
    "return company information from the api" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09CompanyInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(
          Option(mdgCompanyInformationResponse(Sub09Response.withEmailNoTimestamp(testEori)))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        await(connector.getCompanyInformation(testEori)) mustBe Option(companyInformation)
      }
    }

    "return company information noConsent '0' when the field is not present" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09CompanyInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(
          Option(mdgCompanyInformationResponse(Sub09Response.noConsentToDisclosureOfPersonalData(testEori)))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        await(connector.getCompanyInformation(testEori)) mustBe Option(companyInformationNoConsentFalse)
      }
    }

    "return None on failure" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09CompanyInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        await(connector.getCompanyInformation(testEori)) mustBe None
      }
    }
  }

  "getXiEoriInformation" should {
    "return xi eori information from the api" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09XiEoriInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(
          Option(mdgXiEoriInformationResponse(Sub09Response.withEmailAndTimestamp(testEori)))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        connector.getXiEoriInformation(testEori).map {
          xiInfo => xiInfo mustBe Option(xiEoriInformation)
        }
      }
    }

    "return xi eori information from the api when pbeaddress is empty" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09XiEoriInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(
          Option(mdgXiEoriInformationResponse(Sub09Response.noXiEoriAddressInformation(testEori)))))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        connector.getXiEoriInformation(testEori).map {
          xiInfo => xiInfo mustBe Option(xiEoriInformationWithNoAddress)
        }
      }
    }

    "return None on failure" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[MdgSub09XiEoriInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.failed(new ServiceUnavailableException("Boom")))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        connector.getXiEoriInformation(testEori).map {
          xiInfo => xiInfo mustBe None
        }
      }
    }
  }

  "retrieveSubscriptions" should {

    "retrieve the subscriptions when successful response is recieved" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[SubscriptionResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(Option(subsResponseOb)))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        connector.retrieveSubscriptions(TEST_EORI).map {
          res => res mustBe Option(subsResponseOb)
        }
      }
    }

    "return None if error occurrs while retrieving the subscriptions" in new Setup {
      when(requestBuilder.setHeader(any[(String, String)]())).thenReturn(requestBuilder)

      when(requestBuilder.execute(any[HttpReads[SubscriptionResponse]], any[ExecutionContext]))
        .thenReturn(Future.failed(new ServiceUnavailableException("Error occurred")))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        connector.retrieveSubscriptions(TEST_EORI).map {
          res => res mustBe empty
        }
      }
    }
  }

  trait Setup {
    val testEori = "someEori"
    val xiEori = "XI123456789000"
    val companyName = "Example Ltd"
    val consent = "1"

    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    val address: AddressInformation = AddressInformation("Address Line 1", "City", Some("postCode"), "GB")
    val companyInformation: CompanyInformation = CompanyInformation(companyName, consent, address)
    val companyInformationNoConsentFalse: CompanyInformation = CompanyInformation(companyName, "0", address)

    val xiEoriInformation: XiEoriInformation =
      XiEoriInformation(xiEori, consent,
        XiEoriAddressInformation("Example Rd", Some("Example"), Some("GB"), None, Some("AA00 0AA")))

    val xiEoriInformationWithNoAddress: XiEoriInformation =
      XiEoriInformation(xiEori, consent, XiEoriAddressInformation(emptyString))

    val status = "test_status"
    val statusText = "test_status_text"
    val endDate = "2024-10-22"
    val paramName = "POSITION"
    val paramValue = "LINK"
    val returnParameters: Array[ReturnParameters] = Seq(ReturnParameters(paramName, paramValue)).toArray
    val vatIds: Array[VatId] = Seq(VatId(Some(COUNTRY_CODE_GB), Some(VAT_ID))).toArray

    val cdsEstablishmentAddress: CdsEstablishmentAddress = CdsEstablishmentAddress(
      streetAndNumber = "86 Mysore Road",
      city = CITY,
      postalCode = Some("SW11 5RZ"),
      countryCode = "GB")

    val pbeAddress: PbeAddress = PbeAddress(
      pbeAddressLine1 = "address line 1",
      pbeAddressLine2 = Some("address line 2"),
      pbeAddressLine3 = Some("city 1"),
      pbeAddressLine4 = None,
      pbePostCode = Some(POST_CODE))

    val xiSubscription: XiSubscription = XiSubscription(
      XI_EORINo = TEST_XI_EORI_VALUE,
      PBEAddress = Some(pbeAddress),
      establishmentInTheCustomsTerritoryOfTheUnion = Some("1"),
      XI_VATNumber = Some("GB123456789"),
      EU_VATNumber = None,
      XI_ConsentToDisclose = "S",
      XI_SICCode = Some("7600"))

    val contactInformation: ContactInformation = ContactInformation(
      personOfContact = Some("Pepper_Pott"),
      sepCorrAddrIndicator = None,
      streetAndNumber = Some("2nd floor, Alexander House"),
      city = Some(CITY),
      postalCode = Some(POST_CODE),
      countryCode = Some(COUNTRY_CODE_GB),
      telephoneNumber = Some("01702215001"),
      faxNumber = Some("01702215002"),
      emailAddress = Some(EMAIL_ADDRESS),
      emailVerificationTimestamp = Some(TIMESTAMP_STRING))

    val responseCommon: SubResponseCommon = SubResponseCommon(status = status,
      statusText = Some(statusText),
      processingDate = DATE_STRING,
      returnParameters = Some(returnParameters))

    val responseDetail: SubResponseDetail = SubResponseDetail(
      EORINo = Some(TEST_EORI),
      EORIStartDate = Some(DATE_STRING),
      EORIEndDate = Some(endDate),
      CDSFullName = COMPANY_NAME,
      CDSEstablishmentAddress = cdsEstablishmentAddress,
      establishmentInTheCustomsTerritoryOfTheUnion = Some("0"),
      typeOfLegalEntity = Some("0001"),
      contactInformation = Some(contactInformation),
      VATIDs = Some(vatIds),
      thirdCountryUniqueIdentificationNumber = Some(Seq("321", "222").toArray),
      consentToDisclosureOfPersonalData = Some("1"),
      shortName = Some("Robinson"),
      dateOfEstablishment = Some("1963-04-01"),
      typeOfPerson = Some("1"),
      principalEconomicActivity = Some("2000"),
      ETMP_Master_Indicator = true,
      XI_Subscription = Some(xiSubscription))

    val subsDisplayResOb: SubscriptionDisplayResponse = SubscriptionDisplayResponse(responseCommon, responseDetail)
    val subsResponseOb: SubscriptionResponse = SubscriptionResponse(subsDisplayResOb)

    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val app: Application = application.overrides(
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[RequestBuilder].toInstance(requestBuilder)
    ).build()

    val connector: Sub09Connector = app.injector.instanceOf[Sub09Connector]

    def mdgResponse(value: JsValue): MdgSub09Response = MdgSub09Response.sub09Reads.reads(value).get

    def mdgCompanyInformationResponse(value: JsValue): MdgSub09CompanyInformationResponse =
      MdgSub09CompanyInformationResponse.sub09CompanyInformation.reads(value).get

    def mdgXiEoriInformationResponse(value: JsValue): MdgSub09XiEoriInformationResponse =
      MdgSub09XiEoriInformationResponse.sub09XiEoriInformation.reads(value).get
  }
}
