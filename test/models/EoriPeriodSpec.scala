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

import utils.SpecBase
import utils.TestData.{DATE_STRING, TEST_EORI_VALUE}
import play.api.libs.json.{JsResultException, JsSuccess, Json}

class EoriPeriodSpec extends SpecBase {

  "EoriPeriod" should {
    "generate correct output for Json Reads" in new Setup {
      import EoriPeriod.format

      Json.fromJson(Json.parse(eoriPeriodObJsString)) mustBe JsSuccess(eoriPeriodOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(eoriPeriodOb) mustBe Json.parse(eoriPeriodObJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"historyResponse\": \"pending\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[EoriPeriod]
      }
    }

    "generate correct output while calling definedDates" in new Setup {
      assert(eoriPeriodOb.definedDates)
    }
  }

  trait Setup {
    val eoriPeriodOb: EoriPeriod = EoriPeriod(TEST_EORI_VALUE, Some(DATE_STRING), Some(DATE_STRING))

    val eoriPeriodObJsString: String =
      """{"eori":"test_eori","validFrom":"2024-07-22","validUntil":"2024-07-22"}""".stripMargin
  }
}
