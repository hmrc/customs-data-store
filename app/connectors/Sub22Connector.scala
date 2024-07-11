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
import models.UndeliverableInformation
import models.requests.{RequestCommon, RequestDetail, Sub22UpdateVerifiedEmailRequest}
import models.responses.UpdateVerifiedEmailResponse
import play.api.Logging
import services.AuditingService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Sub22Connector @Inject()(httpClient: HttpClient,
                               appConfig: AppConfig,
                               auditingService: AuditingService)
                              (implicit executionContext: ExecutionContext) extends Logging {

  def updateUndeliverable(undeliverableInformation: UndeliverableInformation,
                          verifiedTimestamp: LocalDateTime, attempts: Int)(implicit hc: HeaderCarrier): Future[Boolean] = {

    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)

    val headers = Seq(
      ("Authorization" -> appConfig.sub22BearerToken),
      ("Date" -> localDate),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP"),
      ("Accept" -> "application/json")
    )

    undeliverableInformation.extractEori match {
      case Some(eori) =>
        val detail = RequestDetail.fromEmailAndEori(undeliverableInformation.event.emailAddress, eori, verifiedTimestamp)
        val request = Sub22UpdateVerifiedEmailRequest.fromDetailAndCommon(RequestCommon(), detail)

        httpClient.PUT[Sub22UpdateVerifiedEmailRequest, UpdateVerifiedEmailResponse](
          appConfig.sub22UpdateVerifiedEmailEndpoint, request, headers

        )(implicitly, implicitly, HeaderCarrier(), implicitly).map { response =>

          val isSuccessful = response.updateVerifiedEmailResponse.responseCommon.statusText.isEmpty
          auditingService.auditSub22Request(request, attempts, isSuccessful)
          isSuccessful
        }.recover {
          case _ =>
            auditingService.auditSub22Request(request, attempts, successful = false)
            false
        }

      case None => logger.error("No eori available in undeliverable information"); Future.successful(false)
    }
  }
}
