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

package connectors

import config.AppConfig
import models.UndeliverableInformation
import models.requests.{RequestCommon, RequestDetail, Sub22UpdateVerifiedEmailRequest}
import models.responses.UpdateVerifiedEmailResponse
import org.joda.time.DateTime
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Sub22Connector @Inject()(httpClient: HttpClient, appConfig: AppConfig)(implicit executionContext: ExecutionContext) extends Logging {

  def updateUndeliverable(undeliverableInformation: UndeliverableInformation, verifiedTimestamp: DateTime): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

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
        httpClient.PUT[Sub22UpdateVerifiedEmailRequest, UpdateVerifiedEmailResponse](appConfig.sub22UpdateVerifiedEmailEndpoint, request, headers).map { response =>
          response.updateVerifiedEmailResponse.responseCommon.statusText.isEmpty
        }.recover { case _ => false }
      case None => logger.error("No eori available in undeliverable information"); Future.successful(false)
    }
  }


}
