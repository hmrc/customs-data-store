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

package utils

import models.responses.{CdsEstablishmentAddress, SubResponseDetail}
import models.{EORI, EmailAddress}

import java.time.LocalDateTime

object TestData {
  val EMAIL_ADDRESS_VALUE = "test@test.com"
  val TEST_EORI_VALUE     = "test_eori"
  val TEST_XI_EORI_VALUE  = "XI_EORI_No"
  val TEST_EORI: EORI     = EORI(TEST_EORI_VALUE)

  val DATE_STRING  = "2024-07-22"
  val COMPANY_NAME = "Tony Stark"

  val CITY            = "London"
  val POST_CODE       = "SS99 1AA"
  val COUNTRY_CODE_GB = "GB"

  val EMAIL_ADDRESS: EmailAddress = EmailAddress(EMAIL_ADDRESS_VALUE)
  val TIMESTAMP_STRING            = "2007-03-20T01:02:03Z"
  val VAT_ID                      = "242"

  val AUTH_BEARER_TOKEN_VALUE = "Bearer secret-token"

  val YEAR    = 2024
  val MONTH   = 12
  val DAY     = 15
  val HOURS   = 16
  val MINUTES = 30
  val SECONDS = 35

  val TEST_LOCAL_DATE_TIME: LocalDateTime = LocalDateTime.of(YEAR, MONTH, DAY, HOURS, MINUTES, SECONDS)

  val defaultSubResponseDetails: SubResponseDetail = SubResponseDetail(
    EORINo = None,
    EORIStartDate = None,
    EORIEndDate = None,
    CDSFullName = "Tony Stark",
    CDSEstablishmentAddress = CdsEstablishmentAddress("86 Mysore Road", "London", Some("SW11 5RZ"), "GB"),
    establishmentInTheCustomsTerritoryOfTheUnion = None,
    typeOfLegalEntity = None,
    contactInformation = None,
    VATIDs = None,
    thirdCountryUniqueIdentificationNumber = None,
    consentToDisclosureOfPersonalData = None,
    shortName = None,
    dateOfEstablishment = None,
    typeOfPerson = None,
    principalEconomicActivity = None,
    ETMP_Master_Indicator = false,
    XI_Subscription = None
  )
}
