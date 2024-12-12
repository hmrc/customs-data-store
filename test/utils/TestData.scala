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

import models.{EORI, EmailAddress}

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
}
