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

import models.NotificationEmail
import java.time.LocalDateTime
import play.api.libs.json.{Json, OFormat}

case class NotificationEmailMongo(address: String,
                                  timestamp: LocalDateTime,
                                  undeliverable: Option[UndeliverableInformationMongo]) {
  def toNotificationEmail: NotificationEmail = NotificationEmail(
    address,
    timestamp,
    undeliverable.map(_.toUndeliverableInformation))
}

object NotificationEmailMongo {

  implicit val emailFormat: OFormat[NotificationEmailMongo] = Json.format[NotificationEmailMongo]

  def fromNotificationEmail(notificationEmail: NotificationEmail): NotificationEmailMongo = NotificationEmailMongo(
    address = notificationEmail.address,
    timestamp = notificationEmail.timestamp,
    undeliverable = notificationEmail.undeliverable.map(UndeliverableInformationMongo.fromUndeliverableInformation)
  )
}
