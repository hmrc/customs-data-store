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

package uk.gov.hmrc.customs.datastore.utils
import org.scalatest.{BeforeAndAfterEach, MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customs.datastore.services.MongoSpecSupport


class SpecBase extends WordSpec with MongoSpecSupport with MockitoSugar with MustMatchers with FutureAwaits with DefaultAwaitTimeout with OptionValues with BeforeAndAfterEach {

  def application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> "false")

}
