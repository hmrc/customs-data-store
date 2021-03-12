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

package uk.gov.hmrc.customs.datastore.testOnly.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.customs.datastore.controllers.CustomAuthConnector
import uk.gov.hmrc.customs.datastore.services.EoriHistoryService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.ExecutionContext

@Singleton()
class HistoricEoriController @Inject()(val authConnector: CustomAuthConnector, etmp: EoriHistoryService, cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def mdgHistoricEori(eori: String) = Action.async { implicit req =>
    etmp.testSub21(eori).map(a => Ok(a))
  }

  def mdgGetEmail(eori: String) = Action.async { implicit req =>
    etmp.testSub09(eori).map(a => Ok(a))
  }

}
