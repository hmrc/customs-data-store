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

import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.BodyWritable

case class Sub22UpdateVerifiedEmailRequest(updateVerifiedEmailRequest: Sub22Request)

object Sub22UpdateVerifiedEmailRequest {
  def fromDetailAndCommon(common: RequestCommon, detail: RequestDetail): Sub22UpdateVerifiedEmailRequest =
    Sub22UpdateVerifiedEmailRequest(Sub22Request(common, detail))

  implicit val writes: Writes[Sub22UpdateVerifiedEmailRequest] = Json.writes[Sub22UpdateVerifiedEmailRequest]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}
