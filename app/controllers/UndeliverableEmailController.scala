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

package controllers

import config.AppConfig
import connectors.Sub22Connector
import models.{FailedToProcess, NoDataToProcess, ProcessResult, ProcessSucceeded, UndeliverableInformation}
import org.joda.time.DateTime
import play.api.mvc.{Action, ControllerComponents}
import play.api.{Logger, LoggerLike}
import repositories.EmailRepository
import services.AuditingService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UndeliverableEmailController @Inject()(emailRepository: EmailRepository,
                                             cc: ControllerComponents,
                                             auditingService: AuditingService,
                                             sub22Connector: Sub22Connector,
                                             appConfig: AppConfig)
                                            (implicit executionContext: ExecutionContext) extends BackendController(cc) {
  val log: LoggerLike = Logger(this.getClass)

  def makeUndeliverable(): Action[UndeliverableInformation] = Action.async(parse.json[UndeliverableInformation]) { implicit request =>
    if (appConfig.undeliverableEmailEnabled) {
      request.body.extractEori match {
        case Some(eori) => emailRepository.findAndUpdate(eori, request.body).flatMap {
          case Some(record) =>
            auditingService.auditBouncedEmail(request.body)
            updateSub22(request.body, record.timestamp, eori).map { _ => NoContent }
          case _ => Future.successful(NotFound)
        }.recover { case err => log.error(s"Failed to mark email as undeliverable: ${err.getMessage}"); InternalServerError }
        case None => Future.successful(BadRequest)
      }
    } else Future.successful(NotFound)
  }

  private def updateSub22(undeliverableInformation: UndeliverableInformation, timestamp: DateTime, eori: String): Future[ProcessResult] = {
    sub22Connector.updateUndeliverable(undeliverableInformation, timestamp).flatMap { updateSuccessful =>
      if (updateSuccessful) {
        emailRepository.markAsSuccessful(eori).map { _ => ProcessSucceeded }
      } else {
        emailRepository.resetProcessing(eori).map { _ => FailedToProcess }
      }
    }
  }
}