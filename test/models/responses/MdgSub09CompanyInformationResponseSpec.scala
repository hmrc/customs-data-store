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

import models.AddressInformation
import play.api.libs.json.{JsSuccess, Json}
import utils.SpecBase

class MdgSub09CompanyInformationResponseSpec extends SpecBase {

  "Reads" should {
    "generate correct output" when {

      "response has contactInformation" in new Setup {
        import MdgSub09CompanyInformationResponse.sub09CompanyInformation

        Json.fromJson(Json.parse(sub09ResponseWithContactInfo)) mustBe
          JsSuccess(msgSub09ResponseObWithContactInfo)
      }

      "response has no contactInformation but has CDSEstablishmentAddress" in new Setup {
        import MdgSub09CompanyInformationResponse.sub09CompanyInformation

        Json.fromJson(Json.parse(sub09ResponseWithNoContactInfo)) mustBe
          JsSuccess(msgSub09ResponseObWithNoContactInfo)
      }
    }
  }

  trait Setup {
    val name = "Tony Stark"
    val consent = "1"
    val streetNo1 = "2nd floor, Alexander House"
    val streetNo2 = "86 Mysore Road"
    val city1 = "Southend-on-sea"
    val city2 = "London"
    val postCode1 = "SS99 1AA"
    val postCode2 = "SW11 5RZ"
    val countryCode = "GB"

    val addressInfoForContactInfo: AddressInformation = AddressInformation(
      streetAndNumber = streetNo1,
      city = city1,
      postalCode = Some(postCode1),
      countryCode = countryCode)

    val addressInfoForEstablishmentAddress: AddressInformation = AddressInformation(
      streetAndNumber = streetNo2,
      city = city2,
      postalCode = Some(postCode2),
      countryCode = countryCode)

    val msgSub09ResponseObWithContactInfo: MdgSub09CompanyInformationResponse = MdgSub09CompanyInformationResponse(
      name = name,
      consent = Some(consent),
      address = addressInfoForContactInfo
    )

    val msgSub09ResponseObWithNoContactInfo: MdgSub09CompanyInformationResponse = MdgSub09CompanyInformationResponse(
      name = name,
      consent = Some(consent),
      address = addressInfoForEstablishmentAddress
    )

    val sub09ResponseWithContactInfo: String =
      """{
        |      "EORINo": "testEori",
        |      "EORIStartDate":"1999-01-01",
        |      "EORIEndDate":"2020-01-01",
        |      "CDSFullName": "Tony Stark",
        |      "CDSEstablishmentAddress": {
        |        "streetAndNumber": "86 Mysore Road",
        |        "city": "London",
        |        "postalCode": "SW11 5RZ",
        |        "countryCode": "GB"
        |      },
        |      "establishmentInTheCustomsTerritoryOfTheUnion": "0",
        |      "typeOfLegalEntity": "0001",
        |      "contactInformation": {
        |        "personOfContact": "Pepper Pott",
        |        "streetAndNumber": "2nd floor, Alexander House",
        |        "city": "Southend-on-sea",
        |        "postalCode": "SS99 1AA",
        |        "countryCode": "GB",
        |        "telephoneNumber": "01702215001",
        |        "faxNumber": "01702215002",
        |        "emailAddress": "someemail@mail.com",
        |        "emailVerificationTimestamp" : "2007-03-20T01:02:03Z"
        |      },
        |      "VATIDs": [
        |        {
        |          "countryCode": "GB",
        |          "VATID": "242"
        |        }
        |      ],
        |      "thirdCountryUniqueIdentificationNumber": ["321","222"],
        |      "consentToDisclosureOfPersonalData": "1",
        |      "shortName": "Robinson",
        |      "dateOfEstablishment": "1963-04-01",
        |      "typeOfPerson": "1",
        |      "principalEconomicActivity": "2000",
        |      "ETMP_Master_Indicator": true,
        |      "XI_Subscription": {
        |        "XI_EORINo": "XI_EORI_No",
        |        "PBEAddress": {
        |          "pbeAddressLine1": "address line 1",
        |          "pbeAddressLine2": "address line 2",
        |          "pbeAddressLine3": "city 1",
        |          "pbePostCode": "AA1 1AA"
        |        },
        |        "establishmentInTheCustomsTerritoryOfTheUnion": "1",
        |        "XI_VATNumber": "GB123456789",
        |        "XI_ConsentToDisclose": "S",
        |        "XI_SICCode": "7600"
        |      }
        |    }""".stripMargin

    val sub09ResponseWithNoContactInfo: String =
      """{
        |      "EORINo": "testEori",
        |      "EORIStartDate":"1999-01-01",
        |      "EORIEndDate":"2020-01-01",
        |      "CDSFullName": "Tony Stark",
        |      "CDSEstablishmentAddress": {
        |        "streetAndNumber": "86 Mysore Road",
        |        "city": "London",
        |        "postalCode": "SW11 5RZ",
        |        "countryCode": "GB"
        |      },
        |      "establishmentInTheCustomsTerritoryOfTheUnion": "0",
        |      "typeOfLegalEntity": "0001",
        |      "VATIDs": [
        |        {
        |          "countryCode": "GB",
        |          "VATID": "242"
        |        }
        |      ],
        |      "thirdCountryUniqueIdentificationNumber": ["321","222"],
        |      "consentToDisclosureOfPersonalData": "1",
        |      "shortName": "Robinson",
        |      "dateOfEstablishment": "1963-04-01",
        |      "typeOfPerson": "1",
        |      "principalEconomicActivity": "2000",
        |      "ETMP_Master_Indicator": true,
        |      "XI_Subscription": {
        |        "XI_EORINo": "XI_EORI_No",
        |        "PBEAddress": {
        |          "pbeAddressLine1": "address line 1",
        |          "pbeAddressLine2": "address line 2",
        |          "pbeAddressLine3": "city 1",
        |          "pbePostCode": "AA1 1AA"
        |        },
        |        "establishmentInTheCustomsTerritoryOfTheUnion": "1",
        |        "XI_VATNumber": "GB123456789",
        |        "XI_ConsentToDisclose": "S",
        |        "XI_SICCode": "7600"
        |      }
        |    }""".stripMargin
  }
}
