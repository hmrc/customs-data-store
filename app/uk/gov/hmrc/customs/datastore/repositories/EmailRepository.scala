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

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{ReplaceOptions, Updates}
import uk.gov.hmrc.customs.datastore.domain.{NotificationEmail, UndeliverableInformation}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultEmailRepository @Inject()(
                                        mongoComponent: MongoComponent
                                      )(implicit executionContext: ExecutionContext)
extends PlayMongoRepository[NotificationEmail](
  collectionName = "email-verification",
  mongoComponent = mongoComponent,
  domainFormat = NotificationEmail.emailFormat,
  indexes = Seq()
) with EmailRepository {

  override def get(id: String): Future[Option[NotificationEmail]] =
    collection.find(equal("_id", id)).toSingle.toFutureOption

  override def set(id: String, notificationEmail: NotificationEmail): Future[Boolean] = {
    collection.replaceOne(
      equal("_id", id),
      notificationEmail,
      ReplaceOptions().upsert(true)
    ).toFuture().map(_.wasAcknowledged())
  }

  override def update(undeliverableInformation: UndeliverableInformation): Future[Boolean] = {
    val update = Updates.set("undeliverable", Codecs.toBson(undeliverableInformation))
    collection.updateOne(
      equal("_id", undeliverableInformation.enrolmentValue),
      update
    ).toFuture().map(_.getModifiedCount == 1)
  }
}

trait EmailRepository {
  def get(id: String): Future[Option[NotificationEmail]]

  def set(id: String, notificationEmail: NotificationEmail): Future[Boolean]

  def update(undeliverableInformation: UndeliverableInformation): Future[Boolean]
}
