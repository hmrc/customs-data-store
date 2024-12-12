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

import models.EmailAddress
import utils.SpecBase
import play.api.libs.json.{JsSuccess, Json}
import utils.TestData.EMAIL_ADDRESS_VALUE

class EmailVerifiedResponseSpec extends SpecBase {

  "EmailVerifiedResponse.format" should {

    "return correct result" when {

      "Reads the response" in new Setup {
        import EmailVerifiedResponse.format

        Json.fromJson(Json.parse(emailVerifiedResString)) mustBe JsSuccess(emailVerifiedOb)
        Json.fromJson(Json.parse(emailVerifiedResStringWithNoEmail)) mustBe JsSuccess(emailVerifiedObWithNoEmail)
      }

      "Writes the object" in new Setup {
        Json.toJson(emailVerifiedOb) mustBe Json.parse(emailVerifiedResString)
        Json.toJson(emailVerifiedObWithNoEmail) mustBe Json.parse(emailVerifiedResStringWithNoEmail)
      }
    }
  }

  "EmailUnverifiedResponse.format" should {

    "return correct result" when {

      "Reads the response" in new Setup {
        import EmailUnverifiedResponse.format

        Json.fromJson(Json.parse(emailUnverifiedResString)) mustBe JsSuccess(emailUnverifiedOb)
        Json.fromJson(Json.parse(emailUnverifiedResStringWithNoEmail)) mustBe JsSuccess(emailUnverifiedObWithNoEmail)
      }

      "Writes the object" in new Setup {
        Json.toJson(emailUnverifiedOb) mustBe Json.parse(emailUnverifiedResString)
        Json.toJson(emailUnverifiedObWithNoEmail) mustBe Json.parse(emailUnverifiedResStringWithNoEmail)
      }
    }
  }

  trait Setup {
    val emailVerifiedResString: String            = """{"verifiedEmail":"test@test.com"}""".stripMargin
    val emailVerifiedResStringWithNoEmail: String = """{}""".stripMargin

    val emailUnverifiedResString: String            = """{"unVerifiedEmail":"test@test.com"}""".stripMargin
    val emailUnverifiedResStringWithNoEmail: String = """{}""".stripMargin

    val emailVerifiedOb: EmailVerifiedResponse            = EmailVerifiedResponse(Some(EmailAddress(EMAIL_ADDRESS_VALUE)))
    val emailVerifiedObWithNoEmail: EmailVerifiedResponse = EmailVerifiedResponse(None)

    val emailUnverifiedOb: EmailUnverifiedResponse            = EmailUnverifiedResponse(Some(EmailAddress(EMAIL_ADDRESS_VALUE)))
    val emailUnverifiedObWithNoEmail: EmailUnverifiedResponse = EmailUnverifiedResponse(None)
  }
}
