/*
 * Copyright 2022 HM Revenue & Customs
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
import config.AppConfig
import models._
import models.repositories.{EmailRepositoryResult, FailedToRetrieveEmail, NotificationEmailMongo, SuccessfulEmail, UndeliverableInformationMongo}
import org.mongodb.scala.model.Filters.{equal, lte}
import org.mongodb.scala.model._
import play.api.Logger
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultEmailRepository @Inject()(
                                        mongoComponent: MongoComponent,
                                        appConfig: AppConfig
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

  override def findAndUpdate(id: String, undeliverableInformation: UndeliverableInformation): Future[Option[NotificationEmailMongo]] = {
    val update = Updates.set("undeliverable", Codecs.toBson(UndeliverableInformationMongo.fromUndeliverableInformation(undeliverableInformation)))
    collection.findOneAndUpdate(
      equal("_id", id),
      update
    ).toFutureOption()
  }

  def nextJobs: Future[Seq[NotificationEmailMongo]] = {
    val filter = Filters.and(
      equal("undeliverable.processed", false),
      equal("undeliverable.notifiedApi", false),
      lte("undeliverable.attempts", appConfig.schedulerMaxAttempts))

    for {
      notificationEmails <- collection.find(filter).toFuture()
      _ <- collection.updateMany(filter, Updates.set("undeliverable.processed", true)).toFuture().map(_.wasAcknowledged())
    } yield {
      if (notificationEmails.isEmpty) {
        logger.info("undeliverable email queue is empty")
      } else {
        logger.info(s"Successfully marked ${notificationEmails.size} update for processing")
      }
      notificationEmails
    }
  }

  override def markAsSuccessful(id: String): Future[Boolean] =
    collection.updateOne(equal("_id", id),
      Updates.set("undeliverable.notifiedApi", true)
    ).toFuture().map(_.wasAcknowledged())

  override def resetProcessing(id: String): Future[Boolean] = {
    collection.updateOne(equal("_id", id),
      Updates.combine(
        Updates.inc("undeliverable.attempts", 1),
        Updates.set("undeliverable.processed", false)
      )
    ).toFuture().map(_.wasAcknowledged())
  }
}

trait EmailRepository {
  def get(id: String): Future[Option[NotificationEmail]]

  def set(id: String, notificationEmail: NotificationEmail): Future[EmailRepositoryResult]

  def resetProcessing(id: String): Future[Boolean]

  def markAsSuccessful(id: String): Future[Boolean]

  def nextJobs: Future[Seq[NotificationEmailMongo]]

  def findAndUpdate(id: String, undeliverableInformation: UndeliverableInformation): Future[Option[NotificationEmailMongo]]
}




