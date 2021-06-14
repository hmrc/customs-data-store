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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import connectors.EoriHistoryConnector
import models._
import repositories.HistoricEoriRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EoriHistoryController @Inject()(historicEoriRepository: HistoricEoriRepository,
                                      eoriHistoryConnector: EoriHistoryConnector,
                                      cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends BackendController(cc) {

  def getEoriHistory(eori: String): Action[AnyContent] = Action.async {
    historicEoriRepository.get(eori).flatMap {
      case Some(eoriPeriods) if eoriPeriods.headOption.exists(_.definedDates) =>
        Future.successful(Ok(Json.toJson(EoriHistoryResponse(eoriPeriods))))
      case _ => retrieveAndStoreHistoricEoris(eori).map {
        eoriHistoryResponse => Ok(Json.toJson(eoriHistoryResponse))
      }.recover { case _ => InternalServerError }
    }
  }

  def updateEoriHistory(): Action[EoriPeriod] = Action.async(parse.json[EoriPeriod]) { implicit request =>
    (for {
      updateEoriSucceeded <- historicEoriRepository.set(Seq(request.body))
      eoriHistory <- if (updateEoriSucceeded) eoriHistoryConnector.getHistory(request.body.eori) else throw FailedToUpdateCache
      updateEoriHistorySucceeded <- historicEoriRepository.set(eoriHistory)
    } yield {
      if (updateEoriHistorySucceeded) {
        NoContent
      } else {
        InternalServerError
      }
    }).recover { case _ => InternalServerError }
  }

  private def retrieveAndStoreHistoricEoris(eori: String): Future[EoriHistoryResponse] = {
    for {
      eoriHistory <- eoriHistoryConnector.getHistory(eori)
      updateSucceeded <- historicEoriRepository.set(eoriHistory)
      maybeEoriHistory <- if (updateSucceeded) historicEoriRepository.get(eori) else throw FailedToUpdateCache
      result = maybeEoriHistory match {
        case Some(eoriPeriods) => EoriHistoryResponse(eoriPeriods)
        case None => throw FailedToRetrieveEoriHistoryFromCache
      }
    } yield result
  }

  case class EoriHistoryResponse(eoriHistory: Seq[EoriPeriod])

  object EoriHistoryResponse {
    implicit val format: OFormat[EoriHistoryResponse] = Json.format[EoriHistoryResponse]
  }
}

case object FailedToUpdateCache extends Exception("Failed to update EoriStore with eori on updateEoriHistory")

case object FailedToRetrieveEoriHistoryFromCache extends Exception("Failed to retrieve eori history after upadting cache")