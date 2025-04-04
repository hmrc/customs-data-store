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

import actionbuilders.{AuthorisedRequest, RequestWithEori}
import models.EmailAddress
import models.responses.EmailVerifiedResponse
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import repositories.EmailRepository

class SubscriptionController @Inject() (
  service: SubscriptionService,
  authorisedRequest: AuthorisedRequest,
  emailRepository: EmailRepository,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  val log: Logger = Logger(this.getClass)

  def getVerifiedEmail: Action[AnyContent] = authorisedRequest async { implicit request: RequestWithEori[AnyContent] =>
    emailRepository.get(request.eori.value).flatMap {

      case Some(value) =>
        val verifiedEmail: EmailVerifiedResponse = EmailVerifiedResponse(Some(EmailAddress(value.address)))
        Future.successful(Ok(Json.toJson(verifiedEmail)))

      case None =>
        service
          .getVerifiedEmail(request.eori)
          .map(response => Ok(Json.toJson(response)))
          .recover { case NonFatal(error) =>
            logErrorAndReturnServiceUnavailable(error)
          }
    }
  }

  def getEmail: Action[AnyContent] = authorisedRequest async { implicit request: RequestWithEori[AnyContent] =>
    emailRepository.get(request.eori.value).flatMap {

      case Some(value) =>
        val verifiedEmail: EmailVerifiedResponse = EmailVerifiedResponse(Some(EmailAddress(value.address)))
        Future.successful(Ok(Json.toJson(verifiedEmail)))

      case None =>
        service
          .getEmailAddress(request.eori)
          .map(response => Ok(Json.toJson(response)))
          .recover { case NonFatal(error) =>
            logErrorAndReturnServiceUnavailable(error)
          }
    }
  }

  def getUnverifiedEmail: Action[AnyContent] = authorisedRequest async {
    implicit request: RequestWithEori[AnyContent] =>
      service
        .getUnverifiedEmail(request.eori)
        .map(response => Ok(Json.toJson(response)))
        .recover { case NonFatal(error) =>
          logErrorAndReturnServiceUnavailable(error)
        }
  }

  private def logErrorAndReturnServiceUnavailable(error: Throwable) = {
    log.error(s"getSubscriptions failed: ${error.getMessage}")
    ServiceUnavailable
  }

}
