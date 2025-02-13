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

package models.responses

import utils.SpecBase
import utils.TestData.*
import play.api.libs.json.Json

class SubscriptionResponseSpec extends SpecBase {

  "responseSubscriptionFormat" should {

    "return correct result" when {
      "Reads the response" in new Setup {
        import models.responses.SubscriptionResponse.responseSubscriptionFormat

        val actualObject: SubscriptionResponse = Json.fromJson(Json.parse(subsResponseString)).get

        actualObject.subscriptionDisplayResponse.responseCommon.returnParameters.get mustBe
          subsResponseOb.subscriptionDisplayResponse.responseCommon.returnParameters.get

        actualObject.subscriptionDisplayResponse.responseDetail.XI_Subscription.get mustBe
          subsResponseOb.subscriptionDisplayResponse.responseDetail.XI_Subscription.get

        actualObject.subscriptionDisplayResponse.responseDetail.contactInformation.get mustBe
          subsResponseOb.subscriptionDisplayResponse.responseDetail.contactInformation.get

        actualObject.subscriptionDisplayResponse.responseDetail.CDSEstablishmentAddress mustBe
          subsResponseOb.subscriptionDisplayResponse.responseDetail.CDSEstablishmentAddress
      }

      "Writes the object" in new Setup {
        Json.toJson(subsResponseOb) mustBe Json.parse(subsResponseString)
      }
    }
  }

  trait Setup {
    val subsResponseString: String =
      """{"subscriptionDisplayResponse":{
        |"responseCommon":{
        |"status":"test_status",
        |"processingDate":"2024-07-22",
        |"statusText":"test_status_text",
        |"returnParameters":[{"paramName":"POSITION","paramValue":"LINK"}]
        |},
        |"responseDetail":{"EORIStartDate":"2024-07-22",
        |"typeOfPerson":"1",
        |"CDSEstablishmentAddress":{"streetAndNumber":"86 Mysore Road",
        |"city":"London",
        |"countryCode":"GB",
        |"postalCode":"SW11 5RZ"},
        |"contactInformation":{"city":"London",
        |"telephoneNumber":"01702215001",
        |"personOfContact":"Pepper_Pott",
        |"emailAddress":"test@test.com",
        |"emailVerificationTimestamp":"2007-03-20T01:02:03Z",
        |"postalCode":"SS99 1AA",
        |"countryCode":"GB",
        |"streetAndNumber":"2nd floor, Alexander House",
        |"faxNumber":"01702215002"},
        |"dateOfEstablishment":"1963-04-01",
        |"EORIEndDate":"2024-10-22",
        |"VATIDs":[{"countryCode":"GB","VATID":"242"}],
        |"XI_Subscription":{"XI_ConsentToDisclose":"S",
        |"XI_EORINo":"XI_EORI_No",
        |"XI_SICCode":"7600",
        |"establishmentInTheCustomsTerritoryOfTheUnion":"1",
        |"XI_VATNumber":"GB123456789",
        |"PBEAddress":{"pbeAddressLine1":"address line 1",
        |"pbeAddressLine2":"address line 2",
        |"pbeAddressLine3":"city 1",
        |"pbePostCode":"SS99 1AA"}},
        |"principalEconomicActivity":"2000",
        |"consentToDisclosureOfPersonalData":"1",
        |"shortName":"Robinson",
        |"thirdCountryUniqueIdentificationNumber":["321","222"],
        |"establishmentInTheCustomsTerritoryOfTheUnion":"0",
        |"ETMP_Master_Indicator":true,
        |"EORINo":"test_eori",
        |"CDSFullName":"Tony Stark",
        |"typeOfLegalEntity":"0001"}}}""".stripMargin

    val status     = "test_status"
    val statusText = "test_status_text"
    val endDate    = "2024-10-22"
    val paramName  = "POSITION"
    val paramValue = "LINK"

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

    val subsResponseOb: SubscriptionResponse = SubscriptionResponse(subsDisplayResOb)
  }
}
