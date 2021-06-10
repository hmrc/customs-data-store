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

package uk.gov.hmrc.customs.datastore.connectors

import uk.gov.hmrc.customs.datastore.config.AppConfig
import uk.gov.hmrc.customs.datastore.domain.onwire.HistoricEoriResponse
import uk.gov.hmrc.customs.datastore.domain.{Eori, EoriPeriod}
import uk.gov.hmrc.customs.datastore.services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class EoriHistoryConnector @Inject()(appConfig: AppConfig,
                                     http: HttpClient,
                                     metricsReporter: MetricsReporterService)(implicit ec: ExecutionContext) {

  def getHistory(eori: Eori)(implicit hc: HeaderCarrier): Future[Seq[EoriPeriod]] = {
    metricsReporter.withResponseTimeLogging("mdg.get.eori-history") {
      val url = s"${appConfig.sub21EORIHistoryEndpoint}$eori"
      http.GET[HistoricEoriResponse](url, headers = Seq("Authorization" -> appConfig.sub21BearerToken)).map {
        response =>
          response.getEORIHistoryResponse.responseDetail.EORIHistory
            .map(history => EoriPeriod(history.EORI, history.validFrom, history.validUntil))
      }
    }
  }
}
