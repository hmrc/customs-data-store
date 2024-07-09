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
import models.responses.{MdgSub09CompanyInformationResponse, MdgSub09Response, MdgSub09XiEoriInformationResponse}
import models.{CompanyInformation, NotificationEmail, XiEoriAddressInformation, XiEoriInformation}
import play.api.{Logger, LoggerLike}
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.DateTimeUtils.rfc1123DateTimeFormatter
import utils.Utils.{getUri, emptyString}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class Sub09Connector @Inject()(appConfig: AppConfig,
                               http: HttpClientV2,
                               metricsReporter: MetricsReporterService)(implicit executionContext: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  private val metricsResourceName = "mdg.get.company-information"
  private val defaultConsent = "0"
  private val endPoint = appConfig.sub09GetSubscriptionsEndpoint

  def getSubscriberInformation(eori: String): Future[Option[NotificationEmail]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    metricsReporter.withResponseTimeLogging(metricsResourceName) {
      http.get(getUri(eori, endPoint)).execute[MdgSub09Response].flatMap {

        case MdgSub09Response(Some(email), Some(timestamp)) => Future.successful(
          Option(NotificationEmail(email, timestamp, None)))

        case _ => Future.successful(None)
      }
    }
  }

  def getCompanyInformation(eori: String): Future[Option[CompanyInformation]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    metricsReporter.withResponseTimeLogging(metricsResourceName) {
      http.get(getUri(eori, endPoint)).execute[Option[MdgSub09CompanyInformationResponse]].flatMap {
        response =>
          Future.successful(
            response.map(v => CompanyInformation(v.name, v.consent.getOrElse(defaultConsent), v.address))
          )
      }.recover {
        case e => log.error(s"Failed to retrieve company information with error: $e"); None
      }
    }
  }

  def getXiEoriInformation(eori: String): Future[Option[XiEoriInformation]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    metricsReporter.withResponseTimeLogging(metricsResourceName) {
      http.get(getUri(eori, endPoint))
        .execute[Option[MdgSub09XiEoriInformationResponse]].flatMap {
          response =>
            Future.successful(
              response.map(v => XiEoriInformation(
                v.xiEori,
                v.consent.getOrElse(defaultConsent),
                v.address.getOrElse(XiEoriAddressInformation(emptyString))))
            )
        }.recover {
          case e => log.error(s"Failed to retrieve xi eori information with error: $e"); None
        }
    }
  }

  private def localDate: String = LocalDateTime.now().format(rfc1123DateTimeFormatter)

  private def createHeaders(localDate: String): Seq[(String, String)] = {
    Seq(
      ("Authorization" -> appConfig.sub09BearerToken),
      ("Date" -> localDate),
      ("X-Correlation-ID" -> UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json"))
  }
}
