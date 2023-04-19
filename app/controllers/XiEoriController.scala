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

import cats.data.OptionT
import connectors.Sub09Connector
import models.XiEoriInformation
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.XiEoriInformationRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class XiEoriController @Inject() (xiEoriInformationRepository: XiEoriInformationRepository,
                                  subscriptionInfoConnector: Sub09Connector,
                                  cc: ControllerComponents)
                                 (implicit executionContext: ExecutionContext) extends BackendController(cc) {

  def getXiEoriInformation(eori: String): Action[AnyContent] = Action.async {
    xiEoriInformationRepository.get(eori).flatMap {
      case Some(xiEoriInformation) => Future.successful(Ok(Json.toJson(xiEoriInformation)))
      case None => retrieveXiEoriInformation(eori).map {
        case Some(xiEoriInformation) => Ok(Json.toJson(xiEoriInformation))
        case None => NotFound
      }
    }
  }

  private def retrieveXiEoriInformation(eori: String): Future[Option[XiEoriInformation]] = {
    (for {
      xiEoriInformation <- OptionT(subscriptionInfoConnector.getXiEoriInformation(eori))
      _ <- OptionT.liftF(xiEoriInformationRepository.set(eori, xiEoriInformation))
    } yield xiEoriInformation).value
  }
}
