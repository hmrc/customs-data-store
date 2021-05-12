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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.datastore.domain.{Eori, EoriPeriod}
import uk.gov.hmrc.customs.datastore.repositories.HistoricEoriRepository
import uk.gov.hmrc.customs.datastore.services.{EoriHistoryService, EoriStore}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EoriHistoryController @Inject()(eoriStore: EoriStore,
                                      historicEoriRepository: HistoricEoriRepository,
                                      historyService: EoriHistoryService,
                                      cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends BackendController(cc) {

  def getEoriHistory(eori: String): Action[AnyContent] = Action.async { implicit request =>
    historicEoriRepository.get(eori).flatMap {
      case Some(eoriHistory) if eoriHistory.eoriHistory.headOption.exists(_.definedDates) =>
        Future.successful(Ok(Json.toJson(EoriHistoryResponse(eoriHistory.eoriHistory))))
      case _ => retrieveAndStoreHistoricEoris(eori).map {
        traderData => Ok(Json.toJson(traderData))
      }.recover {case _ => InternalServerError }
    }
  }

  def updateEoriHistory(): Action[EoriPeriod] = Action.async(parse.json[EoriPeriod]) { implicit request =>
    (for {
      updateEoriSucceeded <- eoriStore.upsertByEori(request.body, None)
      eoriHistory <- if (updateEoriSucceeded) historyService.getHistory(request.body.eori) else throw new RuntimeException("Failed to update EoriStore with eori on updateEoriHistory")
      updateEoriHistorySucceeded <- eoriStore.updateHistoricEoris(eoriHistory)
    } yield {
      if (updateEoriHistorySucceeded) { NoContent } else { InternalServerError }
    }).recover{ case _ => InternalServerError}
  }


  private def retrieveAndStoreHistoricEoris(eori: Eori)(implicit hc: HeaderCarrier): Future[EoriHistoryResponse] = {
    for {
      eoriHistory <- historyService.getHistory(eori)
      updateSucceeded <- historicEoriRepository.set(eori, eoriHistory)
      maybeEoriHistory <- if (updateSucceeded) historicEoriRepository.get(eori) else throw new RuntimeException("Updating historic EORI's failed to update cache")
      result = maybeEoriHistory match {
        case Some(eoriHistory) => EoriHistoryResponse(eoriHistory.eoriHistory)
        case None => throw new RuntimeException("Failed to retrieve trader data after updating cache")
      }
    } yield result
  }

  case class EoriHistoryResponse(eoriHistory: Seq[EoriPeriod])

  object EoriHistoryResponse {
    implicit val format: OFormat[EoriHistoryResponse] = Json.format[EoriHistoryResponse]
  }
}
