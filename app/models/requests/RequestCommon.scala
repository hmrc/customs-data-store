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

package models.requests

import play.api.libs.json.{Json, Writes}
import utils.DateTimeUtils.dateTimeWritesIsoUtc
import utils.Utils.{emptyString, hyphen}

import java.time.Clock
import java.util.UUID
import java.time.LocalDateTime

case class RequestCommon(regime: String,
                         receiptDate: LocalDateTime,
                         acknowledgementReference: String)

object RequestCommon {
  val cl = Clock.systemUTC

  def apply(): RequestCommon = RequestCommon("CDS", LocalDateTime.now(cl),
    UUID.randomUUID().toString.replace(hyphen, emptyString)
  )

  implicit val dateTimeWrites: Writes[LocalDateTime] = dateTimeWritesIsoUtc
  implicit val writes: Writes[RequestCommon] = Json.writes[RequestCommon]
}
