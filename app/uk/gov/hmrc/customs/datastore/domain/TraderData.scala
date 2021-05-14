/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.datastore.domain

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json._
import uk.gov.hmrc.customs.datastore.domain.request.UpdateVerifiedEmailRequest

import java.time.LocalDateTime

case class EoriPeriod(eori: Eori,
                      validFrom: Option[String],
                      validUntil: Option[String]) {
  def definedDates: Boolean = validFrom.isDefined || validUntil.isDefined
}

object EoriPeriod {
  implicit val format: OFormat[EoriPeriod] = Json.format[EoriPeriod]
}

case class NotificationEmail(address: Option[EmailAddress],
                             timestamp: Option[Timestamp])


case class TraderData(eoriHistory: Seq[EoriPeriod],
                      notificationEmail:Option[NotificationEmail])

object TraderData {
  implicit val traderDataFormat: OFormat[TraderData] = Json.format[TraderData]
}

object NotificationEmail {
  def fromEmailRequest(updateVerifiedEmailRequest: UpdateVerifiedEmailRequest): NotificationEmail = {
    NotificationEmail(Some(updateVerifiedEmailRequest.address), Some(updateVerifiedEmailRequest.timestamp))
  }

  implicit val emailFormat: OFormat[NotificationEmail] = Json.format[NotificationEmail]
}

case class EoriHistory(eoriHistory: Seq[EoriPeriod], lastUpdated: LocalDateTime = LocalDateTime.now)

object EoriHistory {
  implicit lazy val writes: OWrites[EoriHistory] = {
    (
      (__ \ "eoriHistory").write[Seq[EoriPeriod]] and
        (__ \ "lastUpdated").write(MongoDateTimeFormats.localDateTimeWrite)
      ) (unlift(EoriHistory.unapply))
  }
  implicit lazy val reads: Reads[EoriHistory] = {
    (
      (__ \ "eoriHistory").read[Seq[EoriPeriod]] and
        (__ \ "lastUpdated").read(MongoDateTimeFormats.localDateTimeRead)
      ) (EoriHistory.apply _)
  }
}