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

import java.time.LocalDateTime

class TraderDataSpec extends SpecBase {

  "traderDataFormat" should {
    "generate correct output for Json Reads" in new Setup {
      import TraderData.traderDataFormat

      Json.fromJson(Json.parse(traderObJsString)) mustBe JsSuccess(traderDatOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(traderDatOb) mustBe Json.parse(traderObJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"history\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[TraderData]
      }
    }
  }

  trait Setup {
    val year    = 2024
    val month   = 8
    val day     = 10
    val hours   = 15
    val minutes = 10
    val seconds = 50

    val eoriHistory: EoriPeriod = EoriPeriod(TEST_EORI_VALUE, Some(DATE_STRING), Some(DATE_STRING))

    val traderDatOb: TraderData =
      TraderData(
        Seq(eoriHistory),
        Some(NotificationEmail("test_address", LocalDateTime.of(year, month, day, hours, minutes, seconds), None))
      )

    val traderObJsString: String =
      """{"eoriHistory":[
        |{"eori":"test_eori",
        |"validFrom":"2024-07-22",
        |"validUntil":"2024-07-22"
        |}],
        |"notificationEmail":{"address":"test_address","timestamp":"2024-08-10T15:10:50Z"}}""".stripMargin
  }
}
