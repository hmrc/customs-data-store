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

package uk.gov.hmrc.customs.datastore.domain.onwire

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JodaReads, JodaWrites, JsPath, Reads}
import uk.gov.hmrc.customs.datastore.domain.EmailAddress

/*
 * on the wire format data models for the MDG Sub 09 request (Subscription Request)
 * This will return contact information (addresses, names, phone numbers), company information and CDS related info
 */
case class MdgSub09DataModel(
                              emailAddress: Option[EmailAddress],
                              verifiedTimestamp: Option[DateTime]
                            )

object MdgSub09DataModel {
  //  "emailAddress": "mickey.mouse@disneyland.com",
  //  "emailVerificationTimestamp": "2019-09-06T12:30:59Z"
  implicit val dateTimeFormat: Format[DateTime] = Format[DateTime](JodaReads.DefaultJodaDateTimeReads, JodaWrites.JodaDateTimeWrites)

  implicit val sub09Reads: Reads[MdgSub09DataModel] =
    ((JsPath \\ "emailAddress").readNullable[String] and (JsPath \\ "emailVerificationTimestamp").readNullable[DateTime])(MdgSub09DataModel.apply _)
}
