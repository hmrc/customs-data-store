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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.customs.datastore.domain.NotificationEmail
import uk.gov.hmrc.customs.datastore.domain.request.UpdateVerifiedEmailRequest
import uk.gov.hmrc.customs.datastore.repositories.EmailRepository
import uk.gov.hmrc.customs.datastore.services.SubscriptionInfoService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VerifiedEmailController @Inject()(
                                         emailRepo: EmailRepository,
                                         subscriptionInfoService: SubscriptionInfoService,
                                         cc: ControllerComponents
                                       )(implicit executionContext: ExecutionContext) extends BackendController(cc) {

  def getVerifiedEmail(eori: String): Action[AnyContent] = Action.async { implicit request =>

    def storeDataResult(maybeNotificationEmail: Option[NotificationEmail]): Future[Result] =
      maybeNotificationEmail match {
        case Some(value) => emailRepo.set(eori, value).map { writeSucceeded =>
          if (writeSucceeded) Ok(Json.toJson(value)) else InternalServerError
        }
        case None => Future.successful(NotFound)
      }

    def retrieveNotificationEmail: Future[Result] =
      for {
        sub09Response <- subscriptionInfoService.getSubscriberInformation(eori)
        notificationEmail = sub09Response.map(NotificationEmail.fromMdgSub09Model)
        result <- storeDataResult(notificationEmail)
      } yield result


    emailRepo.get(eori).flatMap {
      case Some(value) => Future.successful(Ok(Json.toJson(value)))
      case None => retrieveNotificationEmail
    }
  }


  def updateVerifiedEmail(): Action[UpdateVerifiedEmailRequest] = Action.async(parse.json[UpdateVerifiedEmailRequest]) {
    implicit request =>
      emailRepo.set(
        request.body.eori,
        NotificationEmail(Some(request.body.address), Some(request.body.timestamp))
      ).map {
        updateSucceeded => if (updateSucceeded) NoContent else InternalServerError
      }
  }
}
