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
import connectors.Sub09Connector
import models.CompanyInformation
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.CompanyInformationRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompanyInformationController @Inject() (
  companyInformationRepository: CompanyInformationRepository,
  subscriptionInfoConnector: Sub09Connector,
  cc: ControllerComponents,
  authorisedRequest: AuthorisedRequest
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  def getCompanyInformation(eori: String): Action[AnyContent] = Action.async {
    retrieveCompanyInformationAndStore(eori).flatMap {
      case Some(companyInformation) => Future.successful(Ok(Json.toJson(companyInformation)))
      case None                     => Future.successful(NotFound)
    }
  }

  def getCompanyInformationV2: Action[AnyContent] = authorisedRequest async {
    implicit request: RequestWithEori[AnyContent] =>
      val eori = request.eori.value

      retrieveCompanyInformationAndStore(eori).flatMap {
        case Some(companyInformation) => Future.successful(Ok(Json.toJson(companyInformation)))
        case None                     => Future.successful(NotFound)
      }
  }

  private def retrieveCompanyInformationAndStore(eori: String): Future[Option[CompanyInformation]] =
    OptionT(companyInformationRepository.get(eori))
      .orElse(
        for {
          companyInformation <- OptionT(subscriptionInfoConnector.getCompanyInformation(eori))
          _                  <- OptionT.liftF(companyInformationRepository.set(eori, companyInformation))
        } yield companyInformation
      )
      .value
}
