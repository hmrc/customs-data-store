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
import models.{CompanyInformation, NotificationEmail, XiEoriAddressInformation, XiEoriInformation}
import models.responses.{MdgSub09CompanyInformationResponse, MdgSub09Response, MdgSub09XiEoriInformationResponse}
import play.api.{Logger, LoggerLike}
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


class Sub09Connector @Inject()(appConfig: AppConfig,
                               http: HttpClient,
                               metricsReporter: MetricsReporterService)(implicit executionContext: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)
  private val acknowledgementRefLength = 32

  def getSubscriberInformation(eori: String): Future[Option[NotificationEmail]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)
    val acknowledgementReference = Random.alphanumeric.take(acknowledgementRefLength).mkString

    val headers = Seq(
      ("Authorization" -> appConfig.sub09BearerToken),
      ("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))

    val uri = url"${
      appConfig.sub09GetSubscriptionsEndpoint
    }?regime=CDS&acknowledgementReference=$acknowledgementReference&EORI=$eori"

    metricsReporter.withResponseTimeLogging("mdg.get.company-information") {
      http.GET[MdgSub09Response](uri, headers = headers).map {
        case MdgSub09Response(Some(email), Some(timestamp)) => Some(NotificationEmail(email, timestamp, None))
        case _ => None
      }
    }
  }

  def getCompanyInformation(eori: String): Future[Option[CompanyInformation]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)
    val acknowledgementReference = Random.alphanumeric.take(acknowledgementRefLength).mkString

    val headers = Seq(
      ("Authorization" -> appConfig.sub09BearerToken),
      ("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))

    val uri = url"${
      appConfig.sub09GetSubscriptionsEndpoint
    }?regime=CDS&acknowledgementReference=$acknowledgementReference&EORI=$eori"

    metricsReporter.withResponseTimeLogging("mdg.get.company-information") {
      http.GET[Option[MdgSub09CompanyInformationResponse]](uri, headers = headers)
        .map(_.map(v => CompanyInformation(v.name, v.consent.getOrElse("0"), v.address))).recover {
        case e => log.error(s"Failed to retrieve company information with error: $e"); None
      }
    }
  }

  def getXiEoriInformation(eori: String): Future[Option[XiEoriInformation]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val emptyString = ""
    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)
    val acknowledgementReference = Random.alphanumeric.take(acknowledgementRefLength).mkString

    val headers = Seq(
      ("Authorization" -> appConfig.sub09BearerToken),
      ("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))

    val uri = url"${
      appConfig.sub09GetSubscriptionsEndpoint
    }?regime=CDS&acknowledgementReference=$acknowledgementReference&EORI=$eori"

    metricsReporter.withResponseTimeLogging("mdg.get.company-information") {
      http.GET[Option[MdgSub09XiEoriInformationResponse]](uri, headers = headers)
        .map(_.map(v =>
          XiEoriInformation(
            v.xiEori,
            v.consent.getOrElse("0"),
            v.address.getOrElse(XiEoriAddressInformation(emptyString)))
        ))
        .recover {
          case e => log.error(s"Failed to retrieve xi eori information with error: $e"); None
        }
    }
  }

}
