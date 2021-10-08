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

package services

import models.{AuditModel, UndeliverableInformation}
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditingService @Inject()(auditConnector: AuditConnector)(implicit executionContext: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  private val AUDIT_SOURCE = "customs-data-store"
  private val BOUNCED_EMAIL_TYPE = "BouncedEmail"
  private val BOUNCED_EMAIL_TRANSACTION_NAME = "Bounced Email"

  implicit val dataEventWrites: Writes[DataEvent] = Json.writes[DataEvent]
  val referrer: HeaderCarrier => String = _.headers(Seq(HeaderNames.REFERER)).headOption.fold("-")(_._2)

  def auditBouncedEmail(undeliverableInformation: UndeliverableInformation)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    audit(AuditModel(BOUNCED_EMAIL_TRANSACTION_NAME, undeliverableInformation.toAuditDetail, BOUNCED_EMAIL_TYPE))
  }

  private def audit(auditModel: AuditModel)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val dataEvent = ExtendedDataEvent(
      auditSource = AUDIT_SOURCE,
      auditType = auditModel.auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(auditModel.transactionName, referrer(hc)),
      detail = auditModel.detail
    )
    log.debug(s"Splunk Audit Event:\n$dataEvent\n")
    auditConnector.sendExtendedEvent(dataEvent)
      .map { auditResult =>
        logAuditResult(auditResult)
        auditResult
      }
  }

  private def logAuditResult(auditResult: AuditResult): Unit = auditResult match {
    case Success =>
      log.debug("Splunk Audit Successful")
    case Failure(err, _) =>
      log.error(s"Splunk Audit Error, message: $err")
    case Disabled =>
      log.debug(s"Auditing Disabled")
  }
}
