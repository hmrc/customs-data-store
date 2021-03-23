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

package uk.gov.hmrc.customs.datastore.services

import javax.inject.Inject
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.datastore.config.AppConfig
import uk.gov.hmrc.customs.datastore.controllers.CircuitBreakerProvider
import uk.gov.hmrc.customs.datastore.domain.onwire.HistoricEoriResponse
import uk.gov.hmrc.customs.datastore.domain.{Eori, EoriPeriod}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._


class EoriHistoryService @Inject()(appConfig: AppConfig, http: HttpClient, metricsReporter: MetricsReporterService)(implicit ec: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  def getHistory(eori: Eori)(implicit hc: HeaderCarrier): Future[Seq[EoriPeriod]] = {
    val hcWithExtraHeaders: HeaderCarrier = hc.copy(authorization = Some(Authorization(appConfig.sub21BearerToken)))

    metricsReporter.withResponseTimeLogging("mdg.get.eori-history") {
      val url = s"${appConfig.sub21EORIHistoryEndpoint}$eori"
      Sub21CircuitBreaker.getEORIHistory(url, hcWithExtraHeaders)
        .transform(
          s => s.getEORIHistoryResponse.responseDetail.EORIHistory.map(history => EoriPeriod(history.EORI, history.validFrom, history.validUntil)),
          f => {
            log.error(f.getMessage, f); f
          }
        ) recover {
        case ex => log.error(ex.getMessage, ex); throw ex
      }

    }
  }

  object Sub21CircuitBreaker extends CircuitBreakerProvider {
    val serviceName = appConfig.sub21ServiceName
    val numberOfCallsToTriggerStateChange = appConfig.sub21NumberOfCallsToSwitchCircuitBreaker
    val unavailablePeriodDuration = appConfig.sub21UnavailablePeriodDuration
    val unstablePeriodDuration = appConfig.sub21UnstablePeriodDuration

    def getEORIHistory(url: String, hcWithExtraHeaders: HeaderCarrier)(implicit hc: HeaderCarrier): Future[HistoricEoriResponse] = {
      withCircuitBreaker {
        http.GET[HistoricEoriResponse](url)(implicitly, hcWithExtraHeaders, implicitly)
      }
    }
  }


}
