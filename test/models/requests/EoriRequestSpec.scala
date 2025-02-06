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

import play.api.libs.json.{JsSuccess, Json}
import utils.SpecBase

class EoriRequestSpec extends SpecBase {

  "EoriRequest.format" should {

    "return correct result" when {
      import EoriRequest.format

      "Reads the request" in new Setup {
        Json.fromJson(Json.parse(eoriString)) mustBe JsSuccess(eoriRequest)
      }

      "Writes the object" in new Setup {
        Json.toJson(eoriRequest) mustBe Json.parse(eoriString)
      }
    }
  }

  trait Setup {
    val eoriString: String       = """{"eori":"testEori"}""".stripMargin
    val eoriRequest: EoriRequest = EoriRequest("testEori")
  }
}
