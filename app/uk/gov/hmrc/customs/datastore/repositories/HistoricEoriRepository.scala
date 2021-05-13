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

package uk.gov.hmrc.customs.datastore.repositories

import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.customs.datastore.domain.{EoriHistory, EoriPeriod}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object Schema {
  val FieldEoriHistory = "eoriHistory"
  val FieldEori = "eori"
  val EoriSearchKey = s"$FieldEoriHistory.$FieldEori"
  val FieldEoriValidFrom = "validFrom"
  val FieldEoriValidUntil = "validUntil"
  val FieldEmails = "notificationEmail"
}

class DefaultHistoricEoriRepository @Inject()(mongo: ReactiveMongoApi, config: Configuration)(implicit executionContext: ExecutionContext) extends HistoricEoriRepository{

  private val collectionName: String = "historic-eoris"
  private val cacheTtl = config.get[Int]("mongodb.timeToLiveInSeconds")

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  private val lastUpdatedIndex = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some(collectionName + "-last-updated-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  override val started: Future[Unit] =
    collection.flatMap {
      _.indexesManager.ensure(lastUpdatedIndex)
    }.map(_ => ())

  override def remove(id: String): Future[Boolean] =
    collection.flatMap(_.delete.one(Json.obj("eoriHistory.eori" -> id))).map(_.ok)



  override def set(eoriHistory: Seq[EoriPeriod]): Future[Boolean] = {
    val selector = Json.obj("eoriHistory.eori" -> Json.obj("$in" -> eoriHistory.map(_.eori)))
    val modifier = Json.obj("$set" -> EoriHistory(eoriHistory))

    collection.flatMap {
      _.update(ordered = false)
        .one(selector, modifier, upsert = true)
        .map(lastError => lastError.ok)
    }
  }

  override def get(id: String): Future[Option[EoriHistory]] =
    collection.flatMap(_.find(Json.obj("eoriHistory.eori" -> id), None).one[EoriHistory])

}

trait HistoricEoriRepository {

  val started: Future[Unit]

  def get(id: String): Future[Option[EoriHistory]]

  def set(eoriHistory: Seq[EoriPeriod]) : Future[Boolean]

  def remove(id: String): Future[Boolean]
}
