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

package models.responses

import models.AddressInformation
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class MdgSub09CompanyInformationResponse(name: String, consent: Option[String], address: AddressInformation)

object MdgSub09CompanyInformationResponse {
  implicit val sub09CompanyInformation: Reads[MdgSub09CompanyInformationResponse] =
    ((JsPath \\ "CDSFullName").read[String] and
      (JsPath \\ "consentToDisclosureOfPersonalData").readNullable[String] and
      (JsPath \\ "CDSEstablishmentAddress").read[AddressInformation]
      ) (MdgSub09CompanyInformationResponse.apply _)
}
