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

import play.api.libs.json.{JsString, Json, OFormat, Writes}
import utils.DateTimeUtils.appendDefaultSeconds

import java.time.LocalDateTime

case class NotificationEmail(address: String,
                             timestamp: LocalDateTime,
                             undeliverable: Option[UndeliverableInformation])

object NotificationEmail {

  implicit val timestampWrites: Writes[LocalDateTime] = {
    Writes[LocalDateTime](d => JsString(s"${appendDefaultSeconds(d.toString)}Z"))
  }

  implicit val emailFormat: OFormat[NotificationEmail] = Json.format[NotificationEmail]
}
