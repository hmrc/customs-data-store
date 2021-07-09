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

package repositories

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{ReplaceOptions, Updates}
import models.{NotificationEmail, UndeliverableInformation}
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

  override def set(id: String, notificationEmail: NotificationEmail): Future[EmailRepositoryResult] = {
    collection.replaceOne(
      equal("_id", id),
      notificationEmail,
      ReplaceOptions().upsert(true)
    ).toFuture().map(v => if (v.wasAcknowledged()) SuccessfulEmail else FailedToRetrieveEmail)
  }

  override def update(id: String, undeliverableInformation: UndeliverableInformation): Future[EmailRepositoryResult] = {
    val update = Updates.set("undeliverable", Codecs.toBson(undeliverableInformation))
    collection.updateOne(
      equal("_id", id),
      update
    ).toFuture().map(v => if(v.getModifiedCount == 1) SuccessfulEmail else NoEmailDocumentsUpdated)
  }
}

trait EmailRepository {
  def get(id: String): Future[Option[NotificationEmail]]
  def set(id: String, notificationEmail: NotificationEmail): Future[EmailRepositoryResult]
  def update(id: String, undeliverableInformation: UndeliverableInformation): Future[EmailRepositoryResult]
}

sealed trait EmailRepositoryResult
case object SuccessfulEmail extends EmailRepositoryResult
case object FailedToRetrieveEmail extends EmailRepositoryResult
case object NoEmailDocumentsUpdated extends EmailRepositoryResult

