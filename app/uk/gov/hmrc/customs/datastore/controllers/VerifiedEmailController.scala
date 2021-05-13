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

package uk.gov.hmrc.customs.datastore.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.datastore.domain.onwire.MdgSub09DataModel
import uk.gov.hmrc.customs.datastore.domain.request.UpdateVerifiedEmailRequest
import uk.gov.hmrc.customs.datastore.domain.{Eori, NotificationEmail}
import uk.gov.hmrc.customs.datastore.repositories.EmailRepository
import uk.gov.hmrc.customs.datastore.services.SubscriptionInfoService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VerifiedEmailController @Inject()(
                                         emailRepo: EmailRepository,
                                         subscriptionInfoService: SubscriptionInfoService,
                                         cc: ControllerComponents
                                       )( implicit executionContext: ExecutionContext ) extends BackendController(cc) {

  def getVerifiedEmail( eori: String ): Action[AnyContent] = Action.async { implicit request =>
    val emailData = for {
      maybeEmailData <- emailRepo.get(eori)
      emailData <- if (maybeEmailData.isDefined) Future.successful(maybeEmailData) else retrieveAndStoreCustomerInformation(eori)
    } yield emailData

    emailData.map {
      case Some(emailData) => Ok(Json.toJson(emailData))
      case None => NotFound
    }.recover { case _ => InternalServerError }
  }

  def updateVerifiedEmail( ): Action[UpdateVerifiedEmailRequest] = Action.async(parse.json[UpdateVerifiedEmailRequest]) {
    implicit request =>
      emailRepo.set(
        request.body.eori,
        NotificationEmail(Some(request.body.address), Some(request.body.timestamp))
      ).map {
          updateSucceeded => if (updateSucceeded) NoContent else InternalServerError
      }
  }

  private def getUpdated( eori: String ) = emailRepo.get(eori) map {
    case Some(v) => Some(v)
    case None => throw new RuntimeException("Failed to retrieve email after updating cache")
  }

  private def writeMsg( eori: String, msg: MdgSub09DataModel ): Future[Option[NotificationEmail]] = {
    emailRepo.set(
      eori,
      NotificationEmail(msg.emailAddress, msg.verifiedTimestamp)
    ).flatMap {
      writeSucceeded => if (writeSucceeded) getUpdated(eori) else throw new RuntimeException("Failed to to update cache")
    }
  }

  private def retrieveAndStoreCustomerInformation( eori: Eori )( implicit hc: HeaderCarrier ) = {
    subscriptionInfoService.getSubscriberInformation(eori).flatMap {
      case None => Future.successful(None)
      case Some(sub09) => writeMsg(eori, sub09)
    }
  }

}
