package models

import play.api.libs.json.{Json, OFormat}

case class UndeliverableInformationTags(enrolment: String, source: String)

object UndeliverableInformationTags {
  implicit val format: OFormat[UndeliverableInformationTags] = Json.format[UndeliverableInformationTags]
}


