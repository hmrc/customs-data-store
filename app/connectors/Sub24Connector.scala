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
import config.Headers.{ACCEPT, AUTHORIZATION, DATE, X_CORRELATION_ID, X_FORWARDED_HOST}
import models.*
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.DateTimeUtils.rfc1123DateTimeFormatter
import utils.Utils.randomUUID

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Sub24Connector @Inject() (appConfig: AppConfig, http: HttpClientV2, metricsReporter: MetricsReporterService)(
  implicit ec: ExecutionContext
) extends Logging {

  private def localDate: String = LocalDateTime.now().format(rfc1123DateTimeFormatter)

  def getEoriHistory(eori: String, gbOnly: Boolean = true): Future[Seq[EoriPeriod]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    metricsReporter.withResponseTimeLogging("mdg.get.eori-history") {
      val url = gbOnly match {
        case true  =>
          if (appConfig.sub24Enabled) url"${appConfig.sub24EORIHistoryEndpoint}?eori=$eori"
          else url"${appConfig.sub21EORIHistoryEndpoint}$eori"
        case false => url"${appConfig.sub24EORIHistoryEndpoint}?eori=$eori&association=1"
      }

      val sub21Headers = Seq(AUTHORIZATION -> appConfig.sub21BearerToken)
      val sub24Headers = Seq(
        AUTHORIZATION    -> appConfig.sub24BearerToken,
        DATE             -> localDate,
        X_CORRELATION_ID -> randomUUID,
        X_FORWARDED_HOST -> "MDTP",
        ACCEPT           -> "application/json"
      )

      val headers = if (appConfig.sub24Enabled) sub24Headers else sub21Headers

      http
        .get(url)
        .setHeader(headers*)
        .execute[HistoricEoriResponse]
        .flatMap { response =>
          Future.successful(
            response.getEORIHistoryResponse.responseDetail.EORIHistory
              .map(history => EoriPeriod(history.EORI, history.validFrom, history.validTo))
          )
        }
        .recoverWith {
          case e @ WithStatusCode(NOT_FOUND) if e.message.contains(NOT_FOUND.toString) =>
            logger.warn("EORI history not found")
            Future.successful(Seq.empty[EoriPeriod])

          case other =>
            logger.error("Unexpected error retrieving EORI history", other)
            Future.failed(other)
        }
    }
  }
}
