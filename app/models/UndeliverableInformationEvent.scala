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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UndeliverableInformationEvent(
  id: String,
  event: String,
  emailAddress: String,
  detected: String,
  code: Option[Int],
  reason: Option[String],
  enrolment: String,
  source: Option[String]
) {

  private val auditCode: String   = code.map(_.toString).getOrElse("-")
  private val auditReason: String = reason.getOrElse("-")

  def toAuditDetail: JsObject = Json.obj(
    "id"           -> id,
    "event"        -> event,
    "emailAddress" -> emailAddress,
    "detected"     -> detected,
    "code"         -> auditCode,
    "reason"       -> auditReason,
    "enrolment"    -> enrolment
  )
}

object UndeliverableInformationEvent {
  implicit val reads: Reads[UndeliverableInformationEvent] =
    ((JsPath \\ "id").read[String] and
      (JsPath \\ "event").read[String] and
      (JsPath \\ "emailAddress").read[String] and
      (JsPath \\ "detected").read[String] and
      (JsPath \\ "code").readNullable[Int] and
      (JsPath \\ "reason").readNullable[String] and
      (JsPath \\ "enrolment").read[String] and
      (JsPath \\ "source").readNullable[String])(UndeliverableInformationEvent.apply)

  implicit val writes: OWrites[UndeliverableInformationEvent] = Json.writes[UndeliverableInformationEvent]
}
