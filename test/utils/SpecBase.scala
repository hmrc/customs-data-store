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


import com.codahale.metrics.MetricRegistry
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.inject.bind
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class SpecBase
  extends AnyWordSpecLike
    with MockitoSugar
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with OptionValues
    with BeforeAndAfterEach {

  def application: GuiceApplicationBuilder = new GuiceApplicationBuilder().overrides(
    bind[Metrics].toInstance(new FakeMetrics)
  ).configure("metrics.enabled" -> "false")

  class FakeMetrics extends Metrics {
    override val defaultRegistry: MetricRegistry = new MetricRegistry
  }
}
