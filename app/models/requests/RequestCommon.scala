/*
 * Copyright 2022 HM Revenue & Customs
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

package models.requests

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Json, Writes}
import utils.DateTimeUtils.dateTimeWritesIsoUtc

import java.time.Clock
import java.util.UUID

case class RequestCommon(regime: String,
                         receiptDate: DateTime,
                         acknowledgementReference: String)

object RequestCommon {
  def apply(): RequestCommon = RequestCommon(
    "CDS",
    new DateTime(Clock.systemUTC().instant().toEpochMilli, DateTimeZone.UTC),
    UUID.randomUUID().toString.replace("-", "")
  )

  implicit val dateTimeWrites: Writes[DateTime] = dateTimeWritesIsoUtc
  implicit val writes: Writes[RequestCommon] = Json.writes[RequestCommon]
}