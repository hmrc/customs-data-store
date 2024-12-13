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

import java.time.LocalDateTime
import play.api.libs.json.{Json, Writes}
import utils.DateTimeUtils.dateTimeWritesIsoUtc

case class RequestDetail(
  IDType: String,
  IDNumber: String,
  emailAddress: String,
  emailVerificationTimestamp: LocalDateTime,
  emailVerified: Boolean
)

object RequestDetail {
  def fromEmailAndEori(email: String, eori: String, timestamp: LocalDateTime): RequestDetail =
    RequestDetail(
      IDType = "EORI",
      IDNumber = eori,
      emailAddress = email,
      emailVerificationTimestamp = timestamp,
      emailVerified = false
    )

  implicit val dateTimeWrites: Writes[LocalDateTime] = dateTimeWritesIsoUtc
  implicit val writes: Writes[RequestDetail]         = Json.writes[RequestDetail]
}
