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

package models.responses

import models.XiEoriAddressInformation
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

case class MdgSub09XiEoriInformationResponse(
                                              xiEori: String,
                                              consent: Option[String],
                                              address: Option[XiEoriAddressInformation]
                                            )

object MdgSub09XiEoriInformationResponse {
  implicit val sub09XiEoriInformation: Reads[MdgSub09XiEoriInformationResponse] =
    ((JsPath \\ "XI_EORINo").read[String] and
      (JsPath \\ "XI_ConsentToDisclose").readNullable[String] and
      (JsPath \\ "PBEAddress").readNullable[XiEoriAddressInformation]
      )(MdgSub09XiEoriInformationResponse.apply _)
}
