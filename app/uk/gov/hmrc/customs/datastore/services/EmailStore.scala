/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.datastore.services

import javax.inject.Inject
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.customs.datastore.domain.TraderData._
import uk.gov.hmrc.customs.datastore.domain.protocol.{Email, Eori}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

class EmailStore @Inject()(mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[Email, BSONObjectID](
    collectionName = "emailStore",
    mongo = mongoComponent.mongoConnector.db,
    domainFormat = Email.jsonFormat,
    idFormat = ReactiveMongoFormats.objectIdFormats
  ) {

  def save(eori: Eori, email: Email)(implicit ec: ExecutionContext): Future[Any] = {
    collection.findAndUpdate(BSONDocument("_id" -> eori.value), email, upsert = true)
  }

  def retrieve(eori: Eori)(implicit ec: ExecutionContext): Future[Option[Email]] = {
    find("_id" -> eori.value).map(_.headOption)
  }
}
