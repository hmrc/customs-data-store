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

package actionbuilders

import utils.SpecBase
import config.AppConfig
import play.api.Application
import uk.gov.hmrc.http.client.HttpClientV2

class AuthorisedRequestSpec extends SpecBase {

  "CustomAuthConnector" should {

    "contain the correct service url" in new Setup {
      customAuthConnector.serviceUrl mustBe appConfig.authUrl
    }

    "contain the correct type of Http client" in new Setup {
      customAuthConnector.httpClientV2.isInstanceOf[HttpClientV2] mustBe true
    }
  }

  trait Setup {
    val app: Application = application.build()

    val customAuthConnector: CustomAuthConnector = app.injector.instanceOf[CustomAuthConnector]
    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  }
}
