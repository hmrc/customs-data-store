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
import cats.data.OptionT
import cats.implicits.*
import connectors.Sub09Connector
import models.NotificationEmail
import models.repositories.SuccessfulEmail
import models.requests.{EoriRequest, UpdateVerifiedEmailRequest}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import repositories.EmailRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VerifiedEmailController @Inject() (
  emailRepo: EmailRepository,
  subscriptionInfoConnector: Sub09Connector,
  authorisedRequest: AuthorisedRequest,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  def getVerifiedEmail(eori: String): Action[AnyContent] = Action.async {
    def retrieveAndStoreEmail: Future[Result] =
      (for {
        notificationEmail <- OptionT(subscriptionInfoConnector.getSubscriberInformation(eori))
        result            <- OptionT.liftF(storeEmail(eori, notificationEmail))
      } yield result).getOrElse(NotFound)

    emailRepo.get(eori).flatMap {
      case Some(value) => Future.successful(Ok(Json.toJson(value)))
      case None        => retrieveAndStoreEmail
    }
  }

  def getVerifiedEmailV2: Action[AnyContent] = authorisedRequest async {
    implicit request: RequestWithEori[AnyContent] =>
      emailRepo.get(request.eori.value).flatMap {
        case Some(value) => Future.successful(Ok(Json.toJson(value)))
        case None        => retrieveAndStoreEmail(request.eori.value)
      }
  }

  def updateVerifiedEmail(): Action[UpdateVerifiedEmailRequest] =
    Action.async(parse.json[UpdateVerifiedEmailRequest]) { implicit request =>
      emailRepo
        .set(
          request.body.eori,
          NotificationEmail(request.body.address, request.body.timestamp, None)
        )
        .map {
          case SuccessfulEmail => NoContent
          case _               => InternalServerError
        }

    }

  def retrieveVerifiedEmailThirdParty(): Action[EoriRequest] = Action.async(parse.json[EoriRequest]) {
    implicit request =>
      val eori = request.body.eori

      emailRepo.get(eori).flatMap {
        case Some(value) => Future.successful(Ok(Json.toJson(value)))
        case None        => retrieveAndStoreEmail(eori)
      }
  }

  private def retrieveAndStoreEmail(eori: String): Future[Result] =
    (for {
      notificationEmail <- OptionT(subscriptionInfoConnector.getSubscriberInformation(eori))
      result            <- OptionT.liftF(storeEmail(eori, notificationEmail))
    } yield result).getOrElse(NotFound)

  private def storeEmail(eori: String, notificationEmail: NotificationEmail): Future[Result] =
    emailRepo.set(eori, notificationEmail).map {
      case SuccessfulEmail => Ok(Json.toJson(notificationEmail))
      case _               => InternalServerError
    }
}
