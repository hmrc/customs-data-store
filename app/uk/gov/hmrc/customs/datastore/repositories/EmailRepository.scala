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
import play.api.libs.json.{Format, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.hmrc.customs.datastore.domain.NotificationEmail
import reactivemongo.play.json.collection.Helpers.idWrites

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailRepository @Inject()(
                               override val mongo: ReactiveMongoApi,
                               override val config: Configuration,
                               override implicit val ec: ExecutionContext
                               ) extends CacheRepository[NotificationEmail] {

  override val collectionName: String = "email-verification"

  override def set(id: String, value: NotificationEmail)(implicit format: Format[NotificationEmail]): Future[Boolean] = {
    val selector = Json.obj(
      "_id" -> id
    )

    val modifier = Json.obj(
      "$set" -> Json.obj("notificationEmail" ->  value)
    )

    collection.flatMap {
      _.update(ordered = false)
        .one(selector, modifier, upsert = true).map {
        lastError => lastError.ok
      }
    }
  }

  //TODO: NOT WORKING!
  // cmd line works: db.getCollection('email-verification').find({"_id":"GB222222213"},{"_id":0,"notificationEmail":1}).pretty()
  override def get(id: String)(implicit format: Format[NotificationEmail]): Future[Option[NotificationEmail]] = {
    val selector = Json.obj("_id" -> id)
    val projection = Some(Json.obj("_id" -> false, "notificationEmail" -> true))
    collection.flatMap(_.find(selector, projection).one[NotificationEmail])
  }

}
