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
import config.Headers.*
import models.responses.SubscriptionResponse
import models.{CompanyInformation, EORI, NotificationEmail, XiEoriInformation}
import play.api.{Logger, LoggerLike}
import services.MetricsReporterService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import utils.DateTimeUtils.rfc1123DateTimeFormatter
import utils.Utils.{randomUUID, uri}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Sub09Connector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  metricsReporter: MetricsReporterService
)(implicit executionContext: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  private val metricsResourceName = "mdg.get.company-information"
  private val defaultConsent      = "0"
  private val endPoint            = appConfig.sub09GetSubscriptionsEndpoint

  private def localDate: String = LocalDateTime.now().format(rfc1123DateTimeFormatter)

  def getSubscriberInformation(eori: String): Future[Option[NotificationEmail]] =
    getSubscriptionAndProcessResponse(eori, "Subscriber Information")
      .map { response =>
        for {
          subDisplayResponse <- response
          detail             <- subDisplayResponse.subscriptionDisplayResponse.responseDetail
          contactInfo        <- detail.contactInformation
          email              <- contactInfo.emailAddress
          timestamp          <- contactInfo.emailVerificationTimestamp
        } yield NotificationEmail(email.value, LocalDateTime.parse(timestamp.stripSuffix("Z")), None)
      }

  def getCompanyInformation(eori: String): Future[Option[CompanyInformation]] =
    getSubscriptionAndProcessResponse(eori, "Company Information")
      .map { response =>
        for {
          subDisplayResponse <- response
          detail             <- subDisplayResponse.subscriptionDisplayResponse.responseDetail
          name                = detail.CDSFullName
          consent            <- detail.consentToDisclosureOfPersonalData orElse Some(defaultConsent)
          contactInfo        <- detail.contactInformation
          address             = contactInfo.toAddress getOrElse detail.CDSEstablishmentAddress.toAddress
        } yield CompanyInformation(name, consent, address)
      }

  def getXiEoriInformation(eori: String): Future[Option[XiEoriInformation]] =
    getSubscriptionAndProcessResponse(eori, "XI Eori Information")
      .map { response =>
        for {
          subDisplayResponse <- response
          detail             <- subDisplayResponse.subscriptionDisplayResponse.responseDetail
          xiSub              <- detail.XI_Subscription
          xiEori              = xiSub.XI_EORINo
          consent             = xiSub.XI_ConsentToDisclose
          address            <- xiSub.PBEAddress
        } yield XiEoriInformation(xiEori, consent, address.toXiEoriAddress)
      }

  def retrieveSubscriptions(eori: EORI): Future[Option[SubscriptionResponse]] =
    getSubscriptionAndProcessResponse(eori.value, "Subscription Response")

  private def getSubscriptionAndProcessResponse(eori: String, infoType: String) = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def processSubscriptionResponse(response: SubscriptionResponse) =
      if (response.subscriptionDisplayResponse.responseDetail.isDefined) {
        Future.successful(Some(response))
      } else {
        log.warn(
          s"SubscriptionResponse retrieved with business error:" +
            s" ${response.subscriptionDisplayResponse.responseCommon.statusText.getOrElse("statusText not available")}"
        )

        Future.successful(None)
      }

    metricsReporter.withResponseTimeLogging(metricsResourceName) {
      httpClient
        .get(uri(eori, endPoint))
        .setHeader(
          AUTHORIZATION    -> appConfig.sub09BearerToken,
          DATE             -> localDate,
          X_CORRELATION_ID -> randomUUID,
          X_FORWARDED_HOST -> "MDTP",
          ACCEPT           -> "application/json"
        )
        .execute[SubscriptionResponse]
        .flatMap(processSubscriptionResponse)
        .recover { case e =>
          log.error(s"Failed to retrieve $infoType with error: $e")
          None
        }
    }
  }
}
