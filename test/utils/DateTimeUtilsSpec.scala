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

import play.api.libs.json.{JsString, Json}
import utils.DateTimeUtils.{appendDefaultSecondsInDateTime, rfc1123DateTimeFormatter, rfc1123DateTimePattern}

import java.time.LocalDateTime

class DateTimeUtilsSpec extends SpecBase {

  "rfc1123DateTimePattern" should {
    "return correct value" in {
      rfc1123DateTimePattern mustBe "EEE, dd MMM yyyy HH:mm:ss z"
    }
  }

  "rfc1123DateTimeFormatter" should {
    "return date in RFC_1123 format" in {
      val year               = 2024
      val month              = 1
      val dayOfMonth         = 10
      val hourOfTheDay       = 8
      val minutesOfTheHour   = 10
      val secondsOfTheMinute = 8

      val date = LocalDateTime.of(year, month, dayOfMonth, hourOfTheDay, minutesOfTheHour, secondsOfTheMinute)

      val result = date.format(rfc1123DateTimeFormatter)

      result.contains("Wed, 10 Jan 2024 08:10:08") mustBe true
    }
  }

  "dateTimeWritesIsoUtc" should {
    "return correct value" in {

      val year               = 2024
      val month              = 1
      val dayOfMonth         = 10
      val hourOfTheDay       = 8
      val minutesOfTheHour   = 10
      val secondsOfTheMinute = 10

      val date = LocalDateTime.of(year, month, dayOfMonth, hourOfTheDay, minutesOfTheHour, secondsOfTheMinute)

      Json.toJson(date)(DateTimeUtils.dateTimeWritesIsoUtc) mustBe JsString("2024-01-10T08:10:10Z")
    }
  }

  "appendDefaultSecondsInDateTime" should {

    "append 00 seconds at the end if seconds' part is missing from DateTime" in {
      appendDefaultSecondsInDateTime("2007-03-20T01:02") mustBe "2007-03-20T01:02:00"
      appendDefaultSecondsInDateTime("2007-03-20T21:44") mustBe "2007-03-20T21:44:00"
    }

    "not modify the DateTime if seconds' part is already present" in {
      appendDefaultSecondsInDateTime("2007-03-20T01:02:46") mustBe "2007-03-20T01:02:46"
      appendDefaultSecondsInDateTime("2007-03-20T12:22:00") mustBe "2007-03-20T12:22:00"
    }
  }
}
