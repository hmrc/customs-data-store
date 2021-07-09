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

import models.UndeliverableInformation
import play.api.mvc.{Action, ControllerComponents}
import repositories.{EmailRepository, FailedToRetrieveEmail, NoEmailDocumentsUpdated, SuccessfulEmail}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UndeliverableEmailController @Inject()(emailRepository: EmailRepository,
                                             cc: ControllerComponents)
                                            (implicit executionContext: ExecutionContext) extends BackendController(cc) {

  def makeUndeliverable(): Action[UndeliverableInformation] = Action.async(parse.json[UndeliverableInformation]) { implicit request =>
    request.body.extractEori match {
      case Some(value) => emailRepository.update(value, request.body).map {
        case SuccessfulEmail => NoContent
        case _ => NotFound
      }.recover { case _ => InternalServerError }
      case None => Future.successful(BadRequest)
    }
  }
}
