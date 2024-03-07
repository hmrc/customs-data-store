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

package models.repositories

import models.{UndeliverableInformation, UndeliverableInformationEvent}
import java.time.LocalDateTime
import play.api.libs.json.{Json, OFormat}

case class UndeliverableInformationMongo(subject: String,
                                         eventId: String,
                                         groupId: String,
                                         timestamp: LocalDateTime,
                                         event: UndeliverableInformationEvent,
                                         notifiedApi: Boolean,
                                         processed: Boolean,
                                         attempts: Int = 0) {

  def toUndeliverableInformation: UndeliverableInformation =
    UndeliverableInformation(subject, eventId, groupId, timestamp, event)
}

object UndeliverableInformationMongo {
  def fromUndeliverableInformation(ui: UndeliverableInformation): UndeliverableInformationMongo =
    UndeliverableInformationMongo(
      ui.subject,
      ui.eventId,
      ui.groupId,
      ui.timestamp,
      ui.event,
      notifiedApi = false,
      processed = false
    )

  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val format: OFormat[UndeliverableInformationMongo] = Json.format[UndeliverableInformationMongo]
}
