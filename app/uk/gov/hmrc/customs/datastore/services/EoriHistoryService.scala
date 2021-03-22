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
import uk.gov.hmrc.customs.datastore.controllers.CircuitBreakerProvider
import uk.gov.hmrc.customs.datastore.domain.onwire.HistoricEoriResponse
import uk.gov.hmrc.customs.datastore.domain.{Eori, EoriPeriod}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.duration.Duration

class EoriHistoryService @Inject()(appConfig: AppConfig, http: HttpClient, metricsReporter: MetricsReporterService)(implicit ec: ExecutionContext)  {

  val log: LoggerLike = Logger(this.getClass)

  def getHistory(eori: Eori)(implicit hc: HeaderCarrier, reads: HttpReads[HistoricEoriResponse]): Future[Seq[EoriPeriod]] = {
    val hcWithExtraHeaders: HeaderCarrier = hc.copy(authorization = Some(Authorization(appConfig.sub21BearerToken)))

    metricsReporter.withResponseTimeLogging("mdg.get.eori-history") {
      val url = s"${appConfig.sub21EORIHistoryEndpoint}$eori"

      Sub21CircuitBreaker.getEORIHistory(url,hcWithExtraHeaders)
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

   object Sub21CircuitBreaker extends CircuitBreakerProvider {
    val serviceName = appConfig.sub21ServiceName
    val numberOfCallsToTriggerStateChange = appConfig.sub21NumberOfCallsToSwitchCircuitBreaker
    val unavailablePeriodDuration = appConfig.sub21UnavailablePeriodDuration
    val unstablePeriodDuration = appConfig.sub21UnstablePeriodDuration

    def getEORIHistory(url: String,hcWithExtraHeaders: HeaderCarrier)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
      withCircuitBreaker {
        val r =  http.GET[HttpResponse](url)(implicitly, hcWithExtraHeaders, implicitly)
         Await.ready(r,Duration.Inf)
        r
      }
    }
  }

  def testSub21(eori: String)(implicit hc: HeaderCarrier, reads: HttpReads[HttpResponse]): Future[JsValue] = {

    val hci: HeaderCarrier = hc
    val sub21Url = appConfig.sub21EORIHistoryEndpoint + eori
    log.info(s"This is a test MDG endpoint : $sub21Url")

    log.info("MDG request headers: " + hci.headers _)
    http.GET[HttpResponse](sub21Url)(reads, hci, implicitly).map(a => Json.parse(a.body))

  }

  def testSub09(eori: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)

    val headers = Seq(("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))

    val hcWithExtraHeaders: HeaderCarrier = hc.copy(authorization = Some(Authorization(appConfig.sub09BearerToken)), extraHeaders = hc.extraHeaders ++ headers)

    log.info("MDG request headers: " + hcWithExtraHeaders)

    val queryParams = Seq(("regime" -> "CDS"), ("acknowledgementReference" -> "21a2b17559e64b14be257a112a7d9e8e"), ("EORI" -> eori))

    val sub09Url = appConfig.sub09GetSubscriptionsEndpoint

    log.info("MDG sub09 URL: " + sub09Url)

    http.GET[HttpResponse](sub09Url, queryParams)(implicitly, hcWithExtraHeaders, implicitly)
      .map { a =>
        log.info(a.body)
        Json.parse(a.body)
      }
  }

}
