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

import com.mongodb.client.model.Indexes.ascending
import models._
import models.repositories.{EmailRepositoryResult, FailedToRetrieveEmail, NoEmailDocumentsUpdated, NotificationEmailMongo, SuccessfulEmail, UndeliverableInformationMongo}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model._
import play.api.Logger
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultEmailRepository @Inject()(
                                        mongoComponent: MongoComponent
                                      )(implicit executionContext: ExecutionContext)
  extends PlayMongoRepository[NotificationEmailMongo](
    collectionName = "email-verification",
    mongoComponent = mongoComponent,
    domainFormat = NotificationEmailMongo.emailFormat,
    indexes = Seq(
      IndexModel(
        ascending("undeliverable.processed"),
        IndexOptions().name("processed-index")
          .sparse(true)
      ))
  ) with EmailRepository {

  private lazy val logger = Logger(this.getClass)

  override def get(id: String): Future[Option[NotificationEmail]] =
    collection.find(equal("_id", id)).toSingle.toFutureOption.map(_.map(_.toNotificationEmail))

  override def set(id: String, notificationEmail: NotificationEmail): Future[EmailRepositoryResult] = {
    collection.replaceOne(
      equal("_id", id),
      NotificationEmailMongo.fromNotificationEmail(notificationEmail),
      ReplaceOptions().upsert(true)
    ).toFuture().map(v => if (v.wasAcknowledged()) SuccessfulEmail else FailedToRetrieveEmail)
  }

  override def update(id: String, undeliverableInformation: UndeliverableInformation): Future[EmailRepositoryResult] = {
    val update = Updates.set("undeliverable", Codecs.toBson(UndeliverableInformationMongo.fromUndeliverableInformation(undeliverableInformation)))
    collection.updateOne(
      equal("_id", id),
      update
    ).toFuture().map(v => if (v.getModifiedCount == 1) SuccessfulEmail else NoEmailDocumentsUpdated)
  }

  override def nextJob: Future[Option[NotificationEmail]] = {
    collection.findOneAndUpdate(
      Filters.and(equal("undeliverable.processed", false), equal("undeliverable.notifiedApi", false)),
      Updates.set("undeliverable.processed", true)
    ).toFutureOption().map {
      case emailJob@Some(_) =>
        logger.info(s"Successfully marked latest undeliverable email for processing")
        emailJob.map(_.toNotificationEmail)
      case None =>
        logger.debug(s"email queue is empty")
        None
    }
  }

  override def markAsSuccessful(id: String): Future[Boolean] =
    collection.updateOne(equal("_id", id),
      Updates.set("undeliverable.notifiedApi", true)
    ).toFuture().map(_.wasAcknowledged())

  override def resetProcessing(id: String): Future[Boolean] =
    collection.updateOne(equal("_id", id),
      Updates.set("undeliverable.processed", false)
    ).toFuture().map(_.wasAcknowledged())
}

trait EmailRepository {
  def get(id: String): Future[Option[NotificationEmail]]

  def set(id: String, notificationEmail: NotificationEmail): Future[EmailRepositoryResult]

  def resetProcessing(id: String): Future[Boolean]

  def markAsSuccessful(id: String): Future[Boolean]

  def nextJob: Future[Option[NotificationEmail]]

  def update(id: String, undeliverableInformation: UndeliverableInformation): Future[EmailRepositoryResult]
}




