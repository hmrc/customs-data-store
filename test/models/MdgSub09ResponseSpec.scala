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

package models

import models.responses.MdgSub09Response
import play.api.libs.json.{JsValue, Json}
import utils.SpecBase

class MdgSub09ResponseSpec extends SpecBase {

  val EORI1 = "testEori"

  "The DataModel" should {
    "parse message with email and a timestamp" in {
      val sub09Response = Sub09Response.withEmailAndTimestamp(EORI1)
      val result = MdgSub09Response.sub09Reads.reads(sub09Response).get
      result.verifiedTimestamp.nonEmpty mustBe true
      result.emailAddress.get mustBe "email@email.com"
    }

    "parse message with email and no timestamp" in {
      val sub09Response = Sub09Response.withEmailNoTimestamp(EORI1)
      val result = MdgSub09Response.sub09Reads.reads(sub09Response).get
      result mustBe MdgSub09Response(Some("email@email.com"), None)
    }

    "parse message no email and no timestamp" in {
      val sub09Response = Sub09Response.noEmailNoTimestamp(EORI1)
      val result = MdgSub09Response.sub09Reads.reads(sub09Response).get
      result mustBe MdgSub09Response(None, None)
    }
  }

}


object Sub09Response {

  private val timeStampKey = "--THE-TIMESTAMP--"
  private val emailKey = "--THE-EMAIL--"
  private val eoriKey = "--THE-EORI-HERE--"
  private val consentToDisclosureOfPersonalDataKEY = "--THE-CONSENT--"

  def withEmailAndTimestamp(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey,""" "emailVerificationTimestamp": "2019-09-06T12:30:59Z",""")
      .replace(consentToDisclosureOfPersonalDataKEY, """ "consentToDisclosureOfPersonalData": "1",""")
    Json.parse(response)
  }

  def withEmailNoTimestamp(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey, "")
      .replace(consentToDisclosureOfPersonalDataKEY, """ "consentToDisclosureOfPersonalData": "1",""")
    Json.parse(response)
  }

  def noEmailNoTimestamp(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, "")
      .replace(timeStampKey, "")
      .replace(consentToDisclosureOfPersonalDataKEY, """ "consentToDisclosureOfPersonalData": "1",""")
    Json.parse(response)
  }

  def noConsentToDisclosureOfPersonalData(eori: String): JsValue = {
    val response = sub09Response(eori)
      .replace(emailKey, """ "emailAddress": "email@email.com", """)
      .replace(timeStampKey,""" "emailVerificationTimestamp": "2019-09-06T12:30:59Z",""")
      .replace(consentToDisclosureOfPersonalDataKEY, "")
    Json.parse(response)
  }

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
       |      "ETMP_Master_Indicator": true
       |    }
       |  }
       |}
    """.stripMargin.replace(eoriKey, eori)

}
