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

import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}

case class UndeliverableInformation(subject: String,
                                    eventId: String,
                                    groupId: String,
                                    timestamp: LocalDateTime,
                                    event: UndeliverableInformationEvent) {

  def toAuditDetail: JsObject = {
    Json.obj(
      "subject" -> subject,
      "eventId" -> eventId,
      "groupId" -> groupId,
      "timestamp" -> timestamp.toString(),
      "event" -> event.toAuditDetail
    )
  }

  def extractEori: Option[String] = event.enrolment.split('~') match {
    case Array(identifier, key, value) if validEnrolment(identifier, key) => Some(value)
    case _ => None
  }

  private def validEnrolment(enrolmentIdentifier: String, enrolmentKey: String): Boolean =
    enrolmentIdentifier.toUpperCase == "HMRC-CUS-ORG" && enrolmentKey.toUpperCase == "EORINUMBER"
}

object UndeliverableInformation {

  implicit val timestampFormat: Format[LocalDateTime] = Format[LocalDateTime](
    Reads[LocalDateTime](js => JsSuccess(LocalDateTime.parse(js.toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))),
    Writes[LocalDateTime](d => {
      println("========== in writes and date is ========"+d.toString)
      JsString(
        d.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
    )
  )
  /*
  implicit val lastUpdatedDateTimeFormat: Format[LocalDateTime] = Format[LocalDateTime](
    Reads[LocalDateTime](js =>
      js.validate[Long] match {
        case JsSuccess(epoc, _) => JsSuccess(Instant.ofEpochMilli(epoc).atOffset(ZoneOffset.UTC).toLocalDateTime)
        case _ =>
          JsSuccess(Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime)
      }
    ),
    Writes[LocalDateTime](d =>
      JsNumber(d.toInstant(ZoneOffset.UTC).toEpochMilli)
    )
  )
   */

  implicit val reads: Reads[UndeliverableInformation] = Json.reads[UndeliverableInformation]
  implicit val writes: OWrites[UndeliverableInformation] = Json.writes[UndeliverableInformation]
}
