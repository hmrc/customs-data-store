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
import play.api.libs.json.{JsString, Json}
import utils.TestData.TEST_EORI_VALUE

class EORISpec extends SpecBase {

  "format" should {

    "read the JsValue correctly" in new Setup {
      import EORI.format

      Json.fromJson(JsString(TEST_EORI_VALUE)).get mustBe eori
    }

    "write the object correctly" in new Setup {
      Json.toJson(eori) mustBe JsString(TEST_EORI_VALUE)
    }
  }

  "pathBinder" should {
    import EORI.pathBinder

    "bind the value correctly" in {
      pathBinder.bind("EORI", TEST_EORI_VALUE) mustBe Right(EORI(TEST_EORI_VALUE))
    }

    "unbind the value correctly" in {
      pathBinder.unbind("EORI", EORI(TEST_EORI_VALUE)) mustBe TEST_EORI_VALUE
    }
  }

  trait Setup {
    val eori: EORI = EORI(TEST_EORI_VALUE)
  }
}
