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

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.datastore.config.AppConfig
import uk.gov.hmrc.customs.datastore.domain.onwire.HistoricEoriResponse
import uk.gov.hmrc.customs.datastore.domain.{Eori, EoriPeriod}
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse,HttpClient}
import scala.concurrent.{ExecutionContext,Future}
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.http.HttpReads.Implicits._

class EoriHistoryService @Inject()(appConfig: AppConfig, http: HttpClient, metricsReporter: MetricsReporterService)(implicit ec: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  def getHistory(eori: Eori)(implicit hc: HeaderCarrier, reads: HttpReads[HistoricEoriResponse]): Future[Seq[EoriPeriod]] = {
    val hci: HeaderCarrier = hc.copy(authorization = Some(Authorization(appConfig.bearerToken)))

    metricsReporter.withResponseTimeLogging("mdg.get.eori-history") {
      val url = s"${appConfig.eoriHistoryUrl}$eori"
      http.GET[HttpResponse](url)(implicitly, hci, implicitly)
        .map { httpResponse =>
          Try(reads.read("GET", url, httpResponse)) match {
            case Success(value) =>
              value.getEORIHistoryResponse.responseDetail.EORIHistory.map {
                history => EoriPeriod(history.EORI, history.validFrom, history.validUntil)
              }
            case Failure(ex) =>  //We did manual json to case class conversion, so that we can write this message on error level
              log.error(ex.getMessage, ex)
              throw ex
          }
        } recover {
          case ex => log.error(ex.getMessage, ex); throw ex
      }
    }
  }

  def testSub21(eori: String)(implicit hc: HeaderCarrier, reads: HttpReads[HttpResponse]): Future[JsValue] = {

    val hci: HeaderCarrier = hc
    val mdgUrl = appConfig.eoriHistoryUrl + eori
    log.info(s"This is a test MDG endpoint : $mdgUrl")

    log.info("MDG request headers: " + hci.headers _)
    http.GET[HttpResponse](mdgUrl)(reads, hci, implicitly).map(a => Json.parse(a.body))

  }

  def testSub09(eori: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)

    val headers = Seq(("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))

    val hcWithExtraHeaders: HeaderCarrier = hc.copy(authorization = Some(Authorization(appConfig.bearerToken)), extraHeaders = hc.extraHeaders ++ headers)

    log.info("MDG request headers: " + hcWithExtraHeaders)

    val queryParams = Seq(("regime" -> "CDS"), ("acknowledgementReference" -> "21a2b17559e64b14be257a112a7d9e8e"), ("EORI" -> eori))

    val mdgUrl = appConfig.companyInformationUrl

    log.info("MDG sub09 URL: " + mdgUrl)

    http.GET[HttpResponse](mdgUrl, queryParams)(implicitly, hcWithExtraHeaders, implicitly)
      .map { a =>
        log.info(a.body)
        Json.parse(a.body)
      }
  }

}
