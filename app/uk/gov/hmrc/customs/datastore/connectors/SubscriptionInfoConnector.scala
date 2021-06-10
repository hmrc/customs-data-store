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
import uk.gov.hmrc.customs.datastore.domain.Eori
import uk.gov.hmrc.customs.datastore.domain.onwire.MdgSub09DataModel
import uk.gov.hmrc.customs.datastore.services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random


class SubscriptionInfoConnector @Inject()(appConfig: AppConfig,
                                          http: HttpClient,
                                          metricsReporter: MetricsReporterService) {

  def getSubscriberInformation(eori: Eori)(implicit hc: HeaderCarrier): Future[Option[MdgSub09DataModel]] = {
    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)
    val acknowledgementReference = Random.alphanumeric.take(32).mkString

    val headers = Seq(
      ("Authorization" -> appConfig.sub09BearerToken),
      ("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))

    val uri = url"${appConfig.sub09GetSubscriptionsEndpoint}?regime=CDS&acknowledgementReference=$acknowledgementReference&EORI=$eori"

    metricsReporter.withResponseTimeLogging("mdg.get.company-information") {
      http.GET[MdgSub09DataModel](uri, headers = headers).map {
        case e@MdgSub09DataModel(Some(_), Some(_)) => Some(e)
        case _ => None
      }
    }
  }
}
