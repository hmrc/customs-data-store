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

import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.http._

trait CircuitBreakerProvider extends UsingCircuitBreaker {

  val serviceName: String
  val numberOfCallsToTriggerStateChange: Int
  val unavailablePeriodDuration: Int
  val unstablePeriodDuration: Int

  protected def circuitBreakerConfig: CircuitBreakerConfig =
    CircuitBreakerConfig(
      serviceName,
      numberOfCallsToTriggerStateChange,
      unavailablePeriodDuration,
      unstablePeriodDuration
    )

  protected def breakOnException(throwable: Throwable): Boolean = {
    throwable match {
      case _: BadRequestException | _: NotFoundException | _: Upstream4xxResponse => false
      case _ => true
    }
  }
}
