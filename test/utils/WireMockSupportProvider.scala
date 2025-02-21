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

package utils

import uk.gov.hmrc.http.test.WireMockSupport
import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, putRequestedFor, urlPathMatching}
import play.api.Configuration
import org.scalatest.Suite

trait WireMockSupportProvider extends WireMockSupport {

  this: Suite =>

  val CONTENT_TYPE = "Content-Type"
  val ACCEPT       = "Accept"
  val MDTP         = "MDTP"

  val AUTH_BEARER_TOKEN_VALUE       = "Bearer secret-token"
  val CONTENT_TYPE_APPLICATION_JSON = "application/json"

  val PARAM_NAME_EORI   = "EORI"
  val PARAM_NAME_eori   = "eori"
  val PARAM_NAME_REGIME = "regime"
  val REGIME_CDS        = "CDS"

  def config: Configuration

  protected def verifyEndPointUrlHit(urlToVerify: String): Unit = wireMockServer.verify(
    getRequestedFor(
      urlPathMatching(urlToVerify)
    )
  )

  protected def verifyEndPointUrlHitWithPut(urlToVerify: String): Unit = wireMockServer.verify(
    putRequestedFor(
      urlPathMatching(urlToVerify)
    )
  )
}
