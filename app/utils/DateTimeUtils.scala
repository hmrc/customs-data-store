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

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.{JsString, Writes}

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {

  val rfc1123DateTimePattern: String = "EEE, dd MMM yyyy HH:mm:ss z"

  val rfc1123DateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(rfc1123DateTimePattern).withZone(ZoneId.systemDefault())

  def dateTimeWritesIsoUtc: Writes[DateTime] = (d: org.joda.time.DateTime) =>
    JsString(d.toString(ISODateTimeFormat.dateTimeNoMillis().withZoneUTC()))
}
