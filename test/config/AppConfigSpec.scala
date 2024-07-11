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

package config

import play.api.Application
import utils.SpecBase

class AppConfigSpec extends SpecBase {

  "url" should {

    "remove left hand side slash" in new Setup {
      import appConfig.URLSyntacticSugar

      val url: String = urlWithTrailingSlash / urlStringWithoutSlashes
      url mustBe urlAfterApplyingStringWithSlash
    }
  }

  "remove right hand side slash" in new Setup {
    import appConfig.URLSyntacticSugar

    val url: String = sampleUrl / urlStringWithLeadingSlash
    url mustBe urlAfterApplyingStringWithSlash
  }

  "remove left and right hand side slashes" in new Setup {
    import appConfig.URLSyntacticSugar

    val url: String = urlWithTrailingSlash / urlStringWithLeadingSlash
    url mustBe urlAfterApplyingStringWithSlash
  }

  "No slashes" in new Setup {
    import appConfig.URLSyntacticSugar

    val url: String = sampleUrl / "abcd"
    url mustBe urlAfterApplyingStringWithSlash
  }

  "schedulerDelay" should {
    "return correct value" in new Setup {
      appConfig.schedulerDelay mustBe 60
    }
  }

  "schedulerMaxAttempts" should {
    "return correct value" in new Setup {
      appConfig.schedulerMaxAttempts mustBe 5
    }
  }

  "sub09GetSubscriptionsEndpoint" should {
    "return correct value" in new Setup {
      appConfig.sub09GetSubscriptionsEndpoint mustBe
        "http://localhost:9753/customs-financials-hods-stub/subscriptions/subscriptiondisplay/v1"
    }
  }

  "sub09BearerToken" should {
    "return correct value" in new Setup {
      appConfig.sub09BearerToken mustBe "Bearer secret-token"
    }
  }

  "sub21EORIHistoryEndpoint" should {
    "return correct value" in new Setup {
      appConfig.sub21EORIHistoryEndpoint mustBe "http://localhost:9753/customs-financials-hods-stub/eorihistory/"
    }
  }

  "sub21BearerToken" should {
    "return correct value" in new Setup {
      appConfig.sub21BearerToken mustBe "Bearer secret-token"
    }
  }

  "sub22UpdateVerifiedEmailEndpoint" should {
    "return correct value" in new Setup {
      appConfig.sub22UpdateVerifiedEmailEndpoint mustBe
        "http://localhost:9753/customs-financials-hods-stub/subscriptions/updateverifiedemail/v1"
    }
  }

  "sub22BearerToken" should {
    "return correct value" in new Setup {
      appConfig.sub22BearerToken mustBe "Bearer secret-token"
    }
  }

  trait Setup {
    val urlStringWithoutSlashes = "abcd"
    val urlStringWithLeadingSlash = "/abcd"
    val sampleUrl = "http://localhost"
    val urlWithTrailingSlash = "http://localhost/"
    val urlAfterApplyingStringWithSlash = "http://localhost/abcd"

    val app: Application = application.build()
    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  }
}
