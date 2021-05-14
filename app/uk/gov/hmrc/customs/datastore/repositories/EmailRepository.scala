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

import akka.http.scaladsl.model.DateTime
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{Json, OWrites, Reads, __}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.Helpers.idWrites
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.customs.datastore.domain.{MongoDateTimeFormats, NotificationEmail}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultEmailRepository @Inject()(
                                        mongo: ReactiveMongoApi
                                      )(implicit executionContext: ExecutionContext) extends EmailRepository {

  private val collectionName: String = "email-verification"

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  val started: Future[Unit] = collection.map(_ => ())

  def get(id: String): Future[Option[NotificationEmail]] = {
    val query = Json.obj("_id" -> id)
    for {
      col <- collection
      result <- col.find(query, None).one[NotificationEmail]
    } yield result
  }

  def set(id: String, notificationEmail: NotificationEmail): Future[Boolean] = {
    val selector = Json.obj("_id" -> id)
    val modifier = Json.obj("$set" -> notificationEmail)

    collection.flatMap {
      _.update(ordered = false)
        .one(selector, modifier, upsert = true)
        .map(lastError => lastError.ok)
    }
  }
}

trait EmailRepository {
  val started: Future[Unit]
  def get(id: String): Future[Option[NotificationEmail]]
  def set(id: String, notificationEmail: NotificationEmail): Future[Boolean]
}
