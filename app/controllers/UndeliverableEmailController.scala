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

package controllers

import connectors.Sub22Connector
import models.repositories.NotificationEmailMongo
import models.{FailedToProcess, NotificationEmail, ProcessResult, ProcessSucceeded, UndeliverableInformation}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import play.api.{Logger, LoggerLike}
import repositories.EmailRepository
import services.AuditingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Utils.emptyString

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UndeliverableEmailController @Inject() (
  emailRepository: EmailRepository,
  cc: ControllerComponents,
  auditingService: AuditingService,
  sub22Connector: Sub22Connector
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {
  val log: LoggerLike = Logger(this.getClass)

  def makeUndeliverable(): Action[UndeliverableInformation] = Action.async(parse.json[UndeliverableInformation]) {
    implicit request =>
      request.body.extractEori match {
        case Some(eori) => retrieveExistingEmailAddressForTheEoriAndProcess(request, eori)
        case None       => Future.successful(BadRequest)
      }
  }

  private def retrieveExistingEmailAddressForTheEoriAndProcess(
    request: Request[UndeliverableInformation],
    eori: String
  )(implicit
    hc: HeaderCarrier
  ) =
    emailRepository.get(eori).flatMap {
      case Some(notifEmail) if notifEmail.address.nonEmpty =>
        checkEmailAddressAndProcessUndeliverableInfo(request, eori, notifEmail.address)
      case _                                               => Future.successful(NotFound)
    }

  private def checkEmailAddressAndProcessUndeliverableInfo(
    request: Request[UndeliverableInformation],
    eori: String,
    existingEmailAddress: String
  )(implicit hc: HeaderCarrier) =
    if (existingEmailAddress == request.body.event.emailAddress) {
      updateUndeliverableInformationForTheEori(request, eori)
    } else {
      log.warn("Mismatch between incoming (undeliverable) and existing (in database) email address")
      Future.successful(NoContent)
    }

  private def updateUndeliverableInformationForTheEori(
    request: Request[UndeliverableInformation],
    eori: String
  )(implicit hc: HeaderCarrier): Future[Result] =
    emailRepository
      .findAndUpdate(eori, request.body)
      .flatMap {
        case Some(record) =>
          auditingService.auditBouncedEmail(request.body)
          updateSub22(request.body, record, eori).map(_ => NoContent)
        case _            => Future.successful(NotFound)
      }
      .recover { case err =>
        log.error(s"Failed to mark email as undeliverable: ${err.getMessage}")
        InternalServerError
      }

  private def updateSub22(
    undeliverableInformation: UndeliverableInformation,
    record: NotificationEmailMongo,
    eori: String
  )(implicit hc: HeaderCarrier): Future[ProcessResult] =
    sub22Connector
      .updateUndeliverable(
        undeliverableInformation,
        record.timestamp,
        record.undeliverable.map(_.attempts).getOrElse(1)
      )
      .flatMap { updateSuccessful =>
        if (updateSuccessful) {
          emailRepository.markAsSuccessful(eori).map(_ => ProcessSucceeded)
        } else {
          emailRepository.resetProcessing(eori).map(_ => FailedToProcess)
        }

      }
}
