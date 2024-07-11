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

package connectors

import config.AppConfig
import models.*
import config.Headers.AUTHORIZATION
import play.api.http.Status.NOT_FOUND
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpErrorFunctions.notFoundMessage
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Sub21Connector @Inject()(appConfig: AppConfig,
                               http: HttpClientV2,
                               metricsReporter: MetricsReporterService)(implicit ec: ExecutionContext) {

  def getEoriHistory(eori: String): Future[Seq[EoriPeriod]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    metricsReporter.withResponseTimeLogging("mdg.get.eori-history") {
      val url = url"${appConfig.sub21EORIHistoryEndpoint}$eori"
      val headers = AUTHORIZATION -> appConfig.sub21BearerToken

      http.get(url).setHeader(headers)
        .execute[HistoricEoriResponse].flatMap {
          response =>
            Future.successful(response.getEORIHistoryResponse.responseDetail.EORIHistory
              .map(history => EoriPeriod(history.EORI, history.validFrom, history.validTo)))
        }.recoverWith {
          case e@WithStatusCode(NOT_FOUND) if e.message.contains(NOT_FOUND.toString) => Future.failed(
            new NotFoundException(notFoundMessage("GET", url.toString, e.message)))
        }
    }
  }
}
