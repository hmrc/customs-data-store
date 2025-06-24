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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.typesafe.config.ConfigFactory
import config.AppConfig
import models.responses.*
import models.*
import org.scalatest.concurrent.ScalaFutures.*
import org.scalatest.matchers.should.Matchers.*
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.*
import play.api.{Application, Configuration}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestData.*
import utils.Utils.emptyString
import utils.{SpecBase, WireMockSupportProvider}

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava

class Sub09ConnectorSpec extends SpecBase with WireMockSupportProvider {

  "getSubscriberInformation" should {
    "return None when the timestamp is not available" in new Setup {

      val withEmailNoTimeStamp: String =
        Json.toJson(Sub09Response.withEmailNoTimestamp(testEori)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(withEmailNoTimeStamp))
      )

      val result: Option[NotificationEmail] = await(connector.getSubscriberInformation(testEori))
      result mustBe empty

      verifyEndPointUrlHit(sub09Url)
    }

    "return Some, when the timestamp is available" in new Setup {
      val withEmailAndTimestampRes: String =
        Json.toJson(Sub09Response.withEmailAndTimestamp(testEori)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(withEmailAndTimestampRes))
      )

      val result: Option[models.NotificationEmail] = connector.getSubscriberInformation(testEori).futureValue

      result.value.address mustBe "email@email.com"

      verifyEndPointUrlHit(sub09Url)
    }

    "return None when the email is not available" in new Setup {

      val noEmailNoTimestampJson: String =
        Json.toJson(Sub09Response.noEmailNoTimestamp(testEori)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(noEmailNoTimestampJson))
      )

      val result: Option[models.NotificationEmail] = connector.getSubscriberInformation(testEori).futureValue
      result mustBe empty

      verifyEndPointUrlHit(sub09Url)
    }
  }

  "getCompanyInformation" should {

    "return company information from the api" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(Json.toJson(subsResponseOb).toString))
      )

      val result: Option[CompanyInformation] = await(connector.getCompanyInformation(testEori))
      result.get mustBe companyInformation

      verifyEndPointUrlHit(sub09Url)
    }

    "return company information noConsent '0' when the field is not present" in new Setup {

      val noConsentToDiscloseResponse: String = Json
        .toJson(
          subsResponseOb.copy(
            subscriptionDisplayResponse = subsDisplayResOb.copy(
              responseDetail = Some(responseDetail.copy(consentToDisclosureOfPersonalData = Some("0")))
            )
          )
        )
        .toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(noConsentToDiscloseResponse))
      )

      val result: Option[CompanyInformation] = await(connector.getCompanyInformation(testEori))
      result mustBe Some(companyInformationNoConsentFalse)

      verifyEndPointUrlHit(sub09Url)
    }

    "return None on failure" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
          )
      )

      val result: Option[models.CompanyInformation] = connector.getCompanyInformation(testEori).futureValue
      result mustBe empty

      verifyEndPointUrlHit(sub09Url)
    }
  }

  "getXiEoriInformation" should {
    "return xi eori information from the api" in new Setup {

      val withEmailAndTimestamp: String =
        Json.toJson(Sub09Response.withEmailAndTimestamp(testEori)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(withEmailAndTimestamp))
      )

      val result: Option[models.XiEoriInformation] = connector.getXiEoriInformation(testEori).futureValue
      result.map(xiInfo => xiInfo mustBe xiEoriInformation)

      verifyEndPointUrlHit(sub09Url)
    }

    "return xi eori information from the api when pbeaddress is empty" in new Setup {

      val noXiEoriAddress: String =
        Json.toJson(Sub09Response.noXiEoriAddressInformation(testEori)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(ok(noXiEoriAddress))
      )

      val result: Option[models.XiEoriInformation] = connector.getXiEoriInformation(testEori).futureValue
      result.map(xiInfo => xiInfo mustBe Option(xiEoriInformationWithNoAddress))

      verifyEndPointUrlHit(sub09Url)
    }

    "return None on failure" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams)
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("""{"error": "Service Unavailable"}""")
          )
      )

      val result: Option[models.XiEoriInformation] = connector.getXiEoriInformation(testEori).futureValue
      result mustBe empty

      verifyEndPointUrlHit(sub09Url)
    }
  }

  "retrieveSubscriptions" should {
    "retrieve the subscriptions when successful response is received" in new Setup {

      val response: String = Json.toJson(Option(subsResponseOb)).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams1)
          .willReturn(ok(response))
      )

      val result: Option[SubscriptionResponse] = connector.retrieveSubscriptions(TEST_EORI).futureValue
      result.map(res => shouldReturnCorrectSubscriptionResponse(res, subsResponseOb))

      verifyEndPointUrlHit(sub09Url)
    }

    "return None when api call is successful and response contains business error" in new Setup {

      val response: String = Json.toJson(subsResponseWithBusinessErrorOb).toString

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams1)
          .willReturn(ok(response))
      )

      val result: Option[SubscriptionResponse] = connector.retrieveSubscriptions(TEST_EORI).futureValue
      result mustBe empty

      verifyEndPointUrlHit(sub09Url)
    }

    "return None if error occurs while retrieving the subscriptions" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(sub09Url))
          .withHeader(X_FORWARDED_HOST, equalTo(MDTP))
          .withHeader(ACCEPT, equalTo(CONTENT_TYPE_APPLICATION_JSON))
          .withHeader(AUTHORIZATION, equalTo(AUTH_BEARER_TOKEN_VALUE))
          .withQueryParams(queryParams1)
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
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
         |    sub09 {
         |      host = $wireMockHost
         |      port = $wireMockPort
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  private def shouldReturnCorrectSubscriptionResponse(
    actualResponse: SubscriptionResponse,
    expectedResponse: SubscriptionResponse
  ) = {

    val actualSubsResponseCommon = actualResponse.subscriptionDisplayResponse.responseCommon
    val actualSubsResponseDetail =
      actualResponse.subscriptionDisplayResponse.responseDetail.getOrElse(defaultSubResponseDetails)

    val expectedSubsResponseCommon  = expectedResponse.subscriptionDisplayResponse.responseCommon
    val expectedSubsResponseDetails =
      expectedResponse.subscriptionDisplayResponse.responseDetail.getOrElse(defaultSubResponseDetails)

    actualSubsResponseCommon.status mustBe expectedSubsResponseCommon.status
    actualSubsResponseCommon.returnParameters.value mustBe expectedSubsResponseCommon.returnParameters.value
    actualSubsResponseCommon.statusText mustBe expectedSubsResponseCommon.statusText
    actualSubsResponseCommon.statusText mustBe expectedSubsResponseCommon.statusText

    actualSubsResponseDetail.EORINo mustBe expectedSubsResponseDetails.EORINo
    actualSubsResponseDetail.VATIDs.value mustBe expectedSubsResponseDetails.VATIDs.value
    actualSubsResponseDetail.contactInformation mustBe expectedSubsResponseDetails.contactInformation
    actualSubsResponseDetail.CDSEstablishmentAddress mustBe expectedSubsResponseDetails.CDSEstablishmentAddress
    actualSubsResponseDetail.dateOfEstablishment mustBe expectedSubsResponseDetails.dateOfEstablishment
    actualSubsResponseDetail.CDSFullName mustBe expectedSubsResponseDetails.CDSFullName
    actualSubsResponseDetail.establishmentInTheCustomsTerritoryOfTheUnion mustBe
      expectedSubsResponseDetails.establishmentInTheCustomsTerritoryOfTheUnion

    actualSubsResponseDetail.consentToDisclosureOfPersonalData mustBe
      expectedSubsResponseDetails.consentToDisclosureOfPersonalData

    actualSubsResponseDetail.EORIEndDate mustBe expectedSubsResponseDetails.EORIEndDate
    actualSubsResponseDetail.EORIStartDate mustBe expectedSubsResponseDetails.EORIStartDate
    actualSubsResponseDetail.ETMP_Master_Indicator mustBe expectedSubsResponseDetails.ETMP_Master_Indicator
    actualSubsResponseDetail.principalEconomicActivity mustBe expectedSubsResponseDetails.principalEconomicActivity
    actualSubsResponseDetail.shortName mustBe expectedSubsResponseDetails.shortName
    actualSubsResponseDetail.thirdCountryUniqueIdentificationNumber.value mustBe
      expectedSubsResponseDetails.thirdCountryUniqueIdentificationNumber.value

    actualSubsResponseDetail.typeOfLegalEntity mustBe expectedSubsResponseDetails.typeOfLegalEntity
    actualSubsResponseDetail.typeOfPerson mustBe expectedSubsResponseDetails.typeOfPerson
    actualSubsResponseDetail.XI_Subscription mustBe expectedSubsResponseDetails.XI_Subscription
  }

  trait Setup {
    val testEori    = "someEori"
    val xiEori      = "XI123456789000"
    val companyName = "Example Ltd"
    val consent     = "1"

    val sub09Url: String = "/customs-financials-hods-stub/subscriptions/subscriptiondisplay/v1"

    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    val queryParams: util.Map[String, StringValuePattern] =
      Map(PARAM_NAME_EORI -> equalTo(testEori), PARAM_NAME_REGIME -> equalTo(REGIME_CDS)).asJava

    val queryParams1: util.Map[String, StringValuePattern] =
      Map(PARAM_NAME_EORI -> equalTo(TEST_EORI_VALUE), PARAM_NAME_REGIME -> equalTo(REGIME_CDS)).asJava

    val xiEoriInformation: XiEoriInformation =
      XiEoriInformation(
        xiEori,
        consent,
        XiEoriAddressInformation("Example Rd", Some("Example"), Some("GB"), None, Some("AA00 0AA"))
      )

    val xiEoriInformationWithNoAddress: XiEoriInformation =
      XiEoriInformation(xiEori, consent, XiEoriAddressInformation(emptyString))

    val status                 = "test_status"
    val statusText             = "test_status_text"
    val statusTextNoFormBundle = "005 - No form bundle found"

    val endDate        = "2024-10-22"
    val paramName      = "POSITION"
    val paramValue     = "LINK"
    val paramFailValue = "FAIL"

    val returnParameters: Array[ReturnParameters]         = Seq(ReturnParameters(paramName, paramValue)).toArray
    val returnParametersWithFail: Array[ReturnParameters] = Seq(ReturnParameters(paramName, paramFailValue)).toArray

    val vatIds: Array[VatId] = Seq(VatId(Some(COUNTRY_CODE_GB), Some(VAT_ID))).toArray

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

    val responseCommonWithBusinessError: SubResponseCommon = SubResponseCommon(
      status = status,
      statusText = Some(statusTextNoFormBundle),
      processingDate = DATE_STRING,
      returnParameters = Some(returnParametersWithFail)
    )

    val responseDetail: SubResponseDetail = SubResponseDetail(
      EORINo = Some(TEST_EORI),
      EORIStartDate = Some(DATE_STRING),
      EORIEndDate = Some(endDate),
      CDSFullName = companyName,
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
      XI_Subscription = Some(xiSubscription)
    )

    val subsDisplayResOb: SubscriptionDisplayResponse =
      SubscriptionDisplayResponse(responseCommon, Some(responseDetail))

    val subsDisplayResWithBusinessErrorOb: SubscriptionDisplayResponse =
      SubscriptionDisplayResponse(responseCommonWithBusinessError, None)

    val subsResponseOb: SubscriptionResponse                  = SubscriptionResponse(subsDisplayResOb)
    val subsResponseWithBusinessErrorOb: SubscriptionResponse = SubscriptionResponse(subsDisplayResWithBusinessErrorOb)

    val companyInformation: CompanyInformation               =
      CompanyInformation(companyName, consent, contactInformation.toAddress.get)
    val companyInformationNoConsentFalse: CompanyInformation =
      CompanyInformation(companyName, "0", contactInformation.toAddress.get)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val app: Application = application
      .configure(config)
      .build()

    val connector: Sub09Connector = app.injector.instanceOf[Sub09Connector]
    val appConfig: AppConfig      = app.injector.instanceOf[AppConfig]
  }
}

object Sub09Response {

  private val timeStampKey                         = "--THE-TIMESTAMP--"
  private val emailKey                             = "--THE-EMAIL--"
  private val eoriKey                              = "--THE-EORI-HERE--"
  private val consentToDisclosureOfPersonalDataKEY = "--THE-CONSENT--"
  private val xiEoriAddressKey                     = "--XI-EORI-ADDRESS--"

  def withEmailAndTimestamp(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey, """ "emailVerificationTimestamp": "2019-09-06T12:30:59Z",""")
      .replace(consentToDisclosureOfPersonalDataKEY, """ "consentToDisclosureOfPersonalData": "1",""")
      .replace(
        xiEoriAddressKey,
        """"PBEAddress": {
          |          "pbeAddressLine1": "Example Rd",
          |          "pbeAddressLine2": "Example",
          |          "pbeAddressLine3": "GB",
          |          "pbePostCode": "AA00 0AA"
          |        },""".stripMargin
      )
    Json.parse(response)
  }

  def withEmailNoTimestamp(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey, emptyString)
      .replace(consentToDisclosureOfPersonalDataKEY, """ "consentToDisclosureOfPersonalData": "1",""")
      .replace(
        xiEoriAddressKey,
        """"PBEAddress": {
          |          "pbeAddressLine1": "Example Rd",
          |          "pbeAddressLine2": "Example",
          |          "pbeAddressLine3": "GB",
          |          "pbePostCode": "AA00 0AA"
          |        },""".stripMargin
      )
    Json.parse(response)
  }

  def noEmailNoTimestamp(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, emptyString)
      .replace(timeStampKey, emptyString)
      .replace(consentToDisclosureOfPersonalDataKEY, """ "consentToDisclosureOfPersonalData": "1",""")
      .replace(
        xiEoriAddressKey,
        """"PBEAddress": {
          |          "pbeAddressLine1": "Example Rd",
          |          "pbeAddressLine2": "Example",
          |          "pbeAddressLine3": "GB",
          |          "pbePostCode": "AA00 0AA"
          |        },""".stripMargin
      )
    Json.parse(response)
  }

  def noConsentToDisclosureOfPersonalData(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey, """ "emailVerificationTimestamp": "2019-09-06T12:30:59Z",""")
      .replace(consentToDisclosureOfPersonalDataKEY, emptyString)
      .replace(
        xiEoriAddressKey,
        """"PBEAddress": {
          |          "pbeAddressLine1": "Example Rd",
          |          "pbeAddressLine2": "Example",
          |          "pbeAddressLine3": "GB",
          |          "pbePostCode": "AA00 0AA"
          |        },""".stripMargin
      )
    Json.parse(response)
  }

  def noXiEoriAddressInformation(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey, """ "emailVerificationTimestamp": "2019-09-06T12:30:59Z",""")
      .replace(consentToDisclosureOfPersonalDataKEY, emptyString)
      .replace(xiEoriAddressKey, emptyString)
    Json.parse(response)
  }

  // scalastyle:off
  protected def sub09Response(eori: String): String =
    s"""
       |{
       |  "subscriptionDisplayResponse": {
       |    "responseCommon": {
       |      "status": "OK",
       |      "statusText": "Optional status text from ETMP",
       |      "processingDate": "2016-08-17T19:33:47Z",
       |      "returnParameters": [
       |        {
       |          "paramName": "POSITION",
       |          "paramValue": "LINK"
       |        }
       |      ]
       |    },
       |    "responseDetail": {
       |      "EORINo": "$eoriKey",
       |      "EORIStartDate": "1999-01-01",
       |      "EORIEndDate": "2020-01-01",
       |      "CDSFullName": "Example Ltd",
       |      "CDSEstablishmentAddress": {
       |        "streetAndNumber": "Example Rd",
       |        "city": "Example",
       |        "postalCode": "AA00 0AA",
       |        "countryCode": "GB"
       |      },
       |      "establishmentInTheCustomsTerritoryOfTheUnion": "0",
       |      "typeOfLegalEntity": "0001",
       |      "contactInformation": {
       |        "personOfContact": "Full Name",
       |        "streetAndNumber": "Address Line 1",
       |        "city": "City",
       |        "postalCode": "postCode",
       |        "countryCode": "GB",
       |        "telephoneNumber": "077999999",
       |        $timeStampKey
       |        $emailKey
       |        "faxNumber": "fax"
       |      },
       |      "VATIDs": [
       |        {
       |          "countryCode": "GB",
       |          "VATID": "VAT-1"
       |        }
       |      ],
       |      "thirdCountryUniqueIdentificationNumber": [
       |        "GB",
       |        "FR"
       |      ],
       |      $consentToDisclosureOfPersonalDataKEY
       |      "shortName": "Mick",
       |      "dateOfEstablishment": "1963-04-01",
       |      "typeOfPerson": "1",
       |      "principalEconomicActivity": "2000",
       |      "ETMP_Master_Indicator": true,
       |      "XI_Subscription": {
       |        "XI_EORINo": "XI123456789000",
       |        $xiEoriAddressKey
       |        "XI_VATNumber": "GB123456789",
       |        "EU_VATNumber": [
       |          { "countryCode": "GB",
       |          "VATId": "123456891012" }
       |        ],
       |        "XI_ConsentToDisclose": "1",
       |        "XI_SICCode": "7600"
       |      }
       |    }
       |  }
       |}
    """.stripMargin.replace(eoriKey, eori)
  // scalastyle:on

}
