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
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson.BSONDocument
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.Helpers.idWrites
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.customs.datastore.domain.MongoDateTimeFormats

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

trait CacheRepository[A] {

  implicit val ec: ExecutionContext
  val mongo: ReactiveMongoApi
  val config: Configuration
  val collectionName: String
  val cacheTtl = config.get[Int]("mongodb.timeToLiveInSeconds")

  def collection: Future[JSONCollection] = mongo.database.map(_.collection[JSONCollection](collectionName))

  private val lastUpdatedIndex = Index(
    key = Seq("lastUpdated" -> IndexType.Ascending),
    name = Some(collectionName + "-last-updated-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  val started: Future[Unit] = collection.flatMap {
      _.indexesManager.ensure(lastUpdatedIndex)
    }.map(_ => ())

  def get(id: String)(implicit format: Format[A]): Future[Option[A]] =
    collection.flatMap(_.find(Json.obj("_id" -> id), None).one[CacheEntry[A]]).map(_.map(_.data))

  def set(id: String, value: A)(implicit format: Format[A]): Future[Boolean] = {
    val selector = Json.obj(
      "_id" -> id
    )

    val modifier = Json.obj(
      "$set" -> CacheEntry(id, value, LocalDateTime.now)
    )

    collection.flatMap {
      _.update(ordered = false)
        .one(selector, modifier, upsert = true).map {
        lastError => lastError.ok
      }
    }
  }

  def clear(id: String): Future[Boolean] =
    collection.flatMap(_.delete.one(Json.obj("_id" -> id)).map(_.ok))

}

case class CacheEntry[T](_id: String, data: T, lastUpdated: LocalDateTime)

object CacheEntry {
  implicit val lastUpdatedReads: Reads[LocalDateTime] = MongoDateTimeFormats.localDateTimeRead
  implicit val lastUpdatedWrites: Writes[LocalDateTime] = MongoDateTimeFormats.localDateTimeWrite

  implicit def cacheEntryFormat[T](implicit format: Format[T]): Format[CacheEntry[T]] = Json.format[CacheEntry[T]]
}
