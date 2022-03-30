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

package models.responses

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class MdgSub09Response(
                             emailAddress: Option[String],
                             verifiedTimestamp: Option[DateTime]
                           )

object MdgSub09Response {
  implicit val dateTimeFormat: Format[DateTime] = Format[DateTime](JodaReads.DefaultJodaDateTimeReads, JodaWrites.JodaDateTimeWrites)

  implicit val sub09Reads: Reads[MdgSub09Response] =
    ((JsPath \\ "emailAddress").readNullable[String] and
      (JsPath \\ "emailVerificationTimestamp").readNullable[DateTime]
      ) (MdgSub09Response.apply _)
}
