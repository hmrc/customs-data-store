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

package uk.gov.hmrc.customs.datastore.controllers

import play.api.{Configuration, Environment}
import uk.gov.hmrc.customs.datastore.config.AppConfig
import uk.gov.hmrc.customs.datastore.utils.SpecBase
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

class CustomAuthConnectorSpec extends SpecBase  {

  val env = Environment.simple()
  val configuration = Configuration.load(env)
  val servicesConfig = new ServicesConfig(configuration)

  implicit val appConfig = new AppConfig(configuration, servicesConfig)

  val mockHttpClient = mock[HttpClient]

  "CustomAuthConnector" should {
    "have service url defined" in {
      val connector = new CustomAuthConnector(appConfig, mockHttpClient)
      connector.serviceUrl mustBe appConfig.authUrl
    }

    "have http defined" in {
      val connector = new CustomAuthConnector(appConfig, mockHttpClient)
      connector.http mustBe mockHttpClient
    }
  }

}
