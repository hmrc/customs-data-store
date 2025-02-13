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
import models.responses.MdgSub09Response.*
import models.{AddressInformation, CompanyInformation, XiEoriAddressInformation, XiEoriInformation}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, ServiceUnavailableException}
import utils.{SpecBase, WireMockSupportProvider}
import utils.Utils.emptyString
import utils.TestData.*
import com.typesafe.config.ConfigFactory
import play.api.{Application, Configuration}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, ok, urlPathMatching}
import config.AppConfig
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.ScalaFutures.*
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.test.Helpers.*
import org.scalatest.matchers.should.Matchers.*

import java.net.URL
import scala.concurrent.ExecutionContext

class Sub09ConnectorSpec extends SpecBase with WireMockSupportProvider {

  "getSubscriberInformation" should {
    "return None when the timestamp is not available" in new Setup {

      val withEmailNoTimeStamp: String =
        Json.toJson(mdgResponse(Sub09Response.withEmailNoTimestamp(testEori))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(withEmailNoTimeStamp))
      )

      val result: Option[models.NotificationEmail] = connector.getSubscriberInformation(testEori).futureValue
      result mustBe None
      verifyEndPointUrlHit(sub09Url)
    }

    "return Some, when the timestamp is available" in new Setup {

      val timeStampResJson: String =
        Json.toJson(mdgResponse(Sub09Response.withEmailAndTimestamp(testEori))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(timeStampResJson))
      )

      val result: Option[models.NotificationEmail] = connector.getSubscriberInformation(testEori).futureValue
      result.value.address mustBe "email@email.com"
      verifyEndPointUrlHit(sub09Url)
    }

    "return None when the email is not available" in new Setup {

      val notAvaResJson: String =
        Json.toJson(mdgResponse(Sub09Response.noEmailNoTimestamp(testEori))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(notAvaResJson))
      )

      val result: Option[models.NotificationEmail] = connector.getSubscriberInformation(testEori).futureValue
      result mustBe None
      verifyEndPointUrlHit(sub09Url)
    }

    /*"propagate ServiceUnavailableException" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub09BearerToken))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("""{"error": "Service Unavailable"}""")
          )
      )

      assertThrows[ServiceUnavailableException] {
        await(connector.getSubscriberInformation(testEori))
      }
    }*/
  }

  "getCompanyInformation" should {
    "return company information from the api" in new Setup {

      val withEmailNoTimeStamp: String =
        Json.toJson(Option(mdgCompanyInformationResponse(Sub09Response.withEmailNoTimestamp(testEori)))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(withEmailNoTimeStamp))
      )

      val result: Option[models.CompanyInformation] = connector.getCompanyInformation(testEori).futureValue
      result.get mustBe companyInformation
      verifyEndPointUrlHit(sub09Url)
    }

    "return company information noConsent '0' when the field is not present" in new Setup {

      val noConsentToDisclose: String =
        Json.toJson(
          Option(mdgCompanyInformationResponse(Sub09Response.noConsentToDisclosureOfPersonalData(testEori)))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(noConsentToDisclose))
      )

      val result: Option[models.CompanyInformation] = connector.getCompanyInformation(testEori).futureValue
      result.get mustBe companyInformationNoConsentFalse
      verifyEndPointUrlHit(sub09Url)
    }

    "return None on failure" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub09BearerToken))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("""{"error": "Service Unavailable"}""")
          )
      )

      val result: Option[models.CompanyInformation] = connector.getCompanyInformation(testEori).futureValue
      result mustBe None
      verifyEndPointUrlHit(sub09Url)
    }
  }

  "getXiEoriInformation" should {
    "return xi eori information from the api" in new Setup {

      val withEmailAndTimestamp: String =
        Json.toJson(
          Option(mdgCompanyInformationResponse(Sub09Response.withEmailAndTimestamp(testEori)))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(withEmailAndTimestamp))
      )

      val result: Option[models.XiEoriInformation] = connector.getXiEoriInformation(testEori).futureValue
      result.map { xiInfo => xiInfo mustBe Option(xiEoriInformation) }
      verifyEndPointUrlHit(sub09Url)
    }

    "return xi eori information from the api when pbeaddress is empty" in new Setup {

      val noXiEoriAddress: String =
        Json.toJson(
          Option(mdgCompanyInformationResponse(Sub09Response.noXiEoriAddressInformation(testEori)))).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(noXiEoriAddress))
      )

      val result: Option[models.XiEoriInformation] = connector.getXiEoriInformation(testEori).futureValue
      result.map { xiInfo => xiInfo mustBe Option(xiEoriInformationWithNoAddress) }
      verifyEndPointUrlHit(sub09Url)
    }

    "return None on failure" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub09BearerToken))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("""{"error": "Service Unavailable"}""")
          )
      )

      val result: Option[models.XiEoriInformation] = connector.getXiEoriInformation(testEori).futureValue
      result mustBe None
      verifyEndPointUrlHit(sub09Url)
    }
  }

  "retrieveSubscriptions" should {
    "retrieve the subscriptions when successful response is recieved" in new Setup {

      val response: String = Json.toJson(Option(subsResponseOb)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .willReturn(ok(response))
      )

      val result: Option[models.responses.SubscriptionResponse] = connector.retrieveSubscriptions(TEST_EORI).futureValue
      result.map { res => res.toString mustBe subsResponseOb.toString }
      verifyEndPointUrlHit(sub09Url)
    }

    "return None if error occurrs while retrieving the subscriptions" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(AUTHORIZATION, equalTo(appConfig.sub09BearerToken))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("""{"error": "Service Unavailable"}""")
          )
      )

      val result: Option[models.responses.SubscriptionResponse] = connector.retrieveSubscriptions(TEST_EORI).futureValue
      result mustBe empty
      verifyEndPointUrlHit(sub09Url)
    }
  }

  override def config: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |      secure-messaging-frontend {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |    }
         |    sub09 {
         |      host = $wireMockHost
         |      port = $wireMockPort
         |      bearer-token = "secret-token"
         |      companyInformationEndpoint = "customs-financials-hods-stub/subscriptions/subscriptiondisplay/v1"
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  trait Setup {
    val testEori    = "someEori"
    val xiEori      = "XI123456789000"
    val companyName = "Example Ltd"
    val consent     = "1"

    val sub09Url: String = "/customs-financials-hods-stub/subscriptions/subscriptiondisplay/v1"

    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    val address: AddressInformation                          = AddressInformation("Address Line 1", "City", Some("postCode"), "GB")
    val companyInformation: CompanyInformation               = CompanyInformation(companyName, consent, address)
    val companyInformationNoConsentFalse: CompanyInformation = CompanyInformation(companyName, "0", address)

    val xiEoriInformation: XiEoriInformation =
      XiEoriInformation(
        xiEori,
        consent,
        XiEoriAddressInformation("Example Rd", Some("Example"), Some("GB"), None, Some("AA00 0AA"))
      )

    val xiEoriInformationWithNoAddress: XiEoriInformation =
      XiEoriInformation(xiEori, consent, XiEoriAddressInformation(emptyString))

    val status                                    = "test_status"
    val statusText                                = "test_status_text"
    val endDate                                   = "2024-10-22"
    val paramName                                 = "POSITION"
    val paramValue                                = "LINK"
    val returnParameters: Seq[ReturnParameters] = Seq(ReturnParameters(paramName, paramValue))
    val vatIds: Seq[VatId]                      = Seq(VatId(Some(COUNTRY_CODE_GB), Some(VAT_ID)))

    val cdsEstablishmentAddress: CdsEstablishmentAddress = CdsEstablishmentAddress(
      streetAndNumber = "86 Mysore Road",
      city = CITY,
      postalCode = Some("SW11 5RZ"),
      countryCode = "GB"
    )

    val pbeAddress: PbeAddress = PbeAddress(
      pbeAddressLine1 = "address line 1",
      pbeAddressLine2 = Some("address line 2"),
      pbeAddressLine3 = Some("city 1"),
      pbeAddressLine4 = None,
      pbePostCode = Some(POST_CODE)
    )

    val xiSubscription: XiSubscription = XiSubscription(
      XI_EORINo = TEST_XI_EORI_VALUE,
      PBEAddress = Some(pbeAddress),
      establishmentInTheCustomsTerritoryOfTheUnion = Some("1"),
      XI_VATNumber = Some("GB123456789"),
      EU_VATNumber = None,
      XI_ConsentToDisclose = "S",
      XI_SICCode = Some("7600")
    )

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
      emailVerificationTimestamp = Some(TIMESTAMP_STRING)
    )

    val responseCommon: SubResponseCommon = SubResponseCommon(
      status = status,
      statusText = Some(statusText),
      processingDate = DATE_STRING,
      returnParameters = Some(returnParameters)
    )

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
      thirdCountryUniqueIdentificationNumber = Some(Seq("321", "222")),
      consentToDisclosureOfPersonalData = Some("1"),
      shortName = Some("Robinson"),
      dateOfEstablishment = Some("1963-04-01"),
      typeOfPerson = Some("1"),
      principalEconomicActivity = Some("2000"),
      ETMP_Master_Indicator = true,
      XI_Subscription = Some(xiSubscription)
    )

    val subsDisplayResOb: SubscriptionDisplayResponse = SubscriptionDisplayResponse(responseCommon, responseDetail)
    val subsResponseOb: SubscriptionResponse          = SubscriptionResponse(subsDisplayResOb)

    implicit val hc: HeaderCarrier     = HeaderCarrier()

    val app: Application = application
      .configure(config)
      .build()

    val actualURL: ArgumentCaptor[URL] = ArgumentCaptor.forClass(classOf[URL])
    val connector: Sub09Connector      = app.injector.instanceOf[Sub09Connector]
    val appConfig: AppConfig           = app.injector.instanceOf[AppConfig]

    def mdgResponse(value: JsValue): MdgSub09Response = MdgSub09Response.sub09Reads.reads(value).get

    def mdgCompanyInformationResponse(value: JsValue): MdgSub09CompanyInformationResponse =
      MdgSub09CompanyInformationResponse.sub09CompanyInformation.reads(value).get

    def mdgXiEoriInformationResponse(value: JsValue): MdgSub09XiEoriInformationResponse =
      MdgSub09XiEoriInformationResponse.sub09XiEoriInformation.reads(value).get
  }
}
