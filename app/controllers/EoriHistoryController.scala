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
import connectors.Sub21Connector
import models.EoriPeriod
import models.requests.EoriRequest
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.{Logger, LoggerLike}
import repositories.{FailedToUpdateHistoricEori, HistoricEoriRepository, HistoricEoriSuccessful}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EoriHistoryController @Inject() (
  historicEoriRepository: HistoricEoriRepository,
  eoriHistoryConnector: Sub21Connector,
  cc: ControllerComponents,
  authorisedRequest: AuthorisedRequest
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  val log: LoggerLike = Logger(this.getClass)

  def getEoriHistory(eori: String): Action[AnyContent] = Action.async {
    historicEoriRepository.get(eori).flatMap {
      case Right(eoriPeriods) if eoriPeriods.headOption.exists(_.definedDates) =>
        Future.successful(Ok(Json.toJson(EoriHistoryResponse(eoriPeriods))))
      case _                                                                   => retrieveAndStoreHistoricEoris(eori)
    }
  }

  def getEoriHistoryV2: Action[AnyContent] = authorisedRequest async { implicit request: RequestWithEori[AnyContent] =>
    val eori = request.eori.value

    historicEoriRepository.get(eori).flatMap {
      case Right(eoriPeriods) if eoriPeriods.headOption.exists(_.definedDates) =>
        Future.successful(Ok(Json.toJson(EoriHistoryResponse(eoriPeriods))))
      case _                                                                   => retrieveAndStoreHistoricEoris(eori)
    }
  }

  def retrieveEoriHistoryThirdParty: Action[EoriRequest] = Action.async(parse.json[EoriRequest]) { implicit request =>
    val eori = request.body.eori

    historicEoriRepository.get(eori).flatMap {
      case Right(eoriPeriods) if eoriPeriods.headOption.exists(_.definedDates) =>
        Future.successful(Ok(Json.toJson(EoriHistoryResponse(eoriPeriods))))
      case _                                                                   => retrieveAndStoreHistoricEoris(eori)
    }
  }

  def updateEoriHistory(): Action[EoriPeriod] = Action.async(parse.json[EoriPeriod]) { implicit request =>
    eoriHistoryConnector
      .getEoriHistory(request.body.eori)
      .flatMap {
        case Nil =>
          log.warn("No EORI history found for EORI - skipping cache update")
          Future.successful(NotFound)

        case eoriHistory =>
          for {
            updateEoriSucceeded        <- historicEoriRepository.set(Seq(request.body))
            updateEoriHistorySucceeded <- updateEoriSucceeded match {
                                            case HistoricEoriSuccessful => historicEoriRepository.set(eoriHistory)
                                            case _                      => Future.successful(FailedToUpdateHistoricEori)
                                          }
          } yield updateEoriHistorySucceeded match {
            case HistoricEoriSuccessful => NoContent
            case _                      => InternalServerError
          }
      }
      .recover { case err =>
        log.info(s"Failed to find EoriHistory: ${err.getMessage}")
        if (err.getMessage.contains("Not found") || err.getMessage.contains("Not Found")) {
          NotFound
        } else {
          InternalServerError
        }
      }
  }

  private def retrieveAndStoreHistoricEoris(eori: String): Future[Result] =
    eoriHistoryConnector
      .getEoriHistory(eori)
      .flatMap {
        case Nil =>
          log.warn("No EORI history found for EORI - skipping cache update")
          Future.successful(Ok(Json.toJson(EoriHistoryResponse(Seq()))))

        case eoriHistory =>
          for {
            updateResult <- historicEoriRepository.set(eoriHistory)
            result       <- updateResult match {
                              case HistoricEoriSuccessful =>
                                historicEoriRepository.get(eori).flatMap {
                                  case Left(_)      => Future.successful(InternalServerError)
                                  case Right(value) => Future.successful(Ok(Json.toJson(EoriHistoryResponse(value))))
                                }
                              case _                      => Future.successful(InternalServerError)
                            }
          } yield result
      }
      .recover { case ex =>
        log.error("Unexpected error retrieving or storing EORI")
        InternalServerError
      }

  case class EoriHistoryResponse(eoriHistory: Seq[EoriPeriod])

  private object EoriHistoryResponse {
    implicit val format: OFormat[EoriHistoryResponse] = Json.format[EoriHistoryResponse]
  }
}
