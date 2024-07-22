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

import utils.SpecBase
import utils.TestData.TEST_EORI_VALUE
import play.api.libs.json.{JsString, Json}

class EmailAddressSpec extends SpecBase {

  "format" should {

    "read the JsValue correctly" in new Setup {
      import EmailAddress.format

      Json.fromJson(JsString(TEST_EORI_VALUE)).get mustBe emailAddress
    }

    "write the object correctly" in new Setup {
      Json.toJson(emailAddress) mustBe JsString(TEST_EORI_VALUE)
    }
  }

  trait Setup {
    val emailAddress: EmailAddress = EmailAddress(TEST_EORI_VALUE)
  }
}
