/*
 * Copyright 2023 HM Revenue & Customs
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

import com.mongodb.WriteConcern
import config.AppConfig
import org.mongodb.scala.SingleObservableFuture
import models.repositories.{
  FailedToRetrieveEmail, NotificationEmailMongo, SuccessfulEmail, UndeliverableInformationMongo
}
import models.{NotificationEmail, UndeliverableInformation, UndeliverableInformationEvent}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.time.LocalDateTime
import org.mongodb.scala.MongoCollection
import play.api.{Application, inject}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.CollectionFactory
import utils.SpecBase

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailRepositorySpec extends SpecBase {

  "successfully retrieve email from repository" in new Setup {

    override val notificationEmail: NotificationEmail =
      NotificationEmail("123", dateTime, Option(undeliverableInformation))

    repository.set(eori, notificationEmail).map { result =>
      result mustBe SuccessfulEmail
    }
  }

  "fail to retrieve email from repository after setting bad email" in new Setup {

    override val notificationEmail: NotificationEmail =
      NotificationEmail("123", dateTime, Some(undeliverableInformation))

    repoWithUnacknowledgedWrite.set(eori, notificationEmail).map { result =>
      result mustBe FailedToRetrieveEmail
    }
  }

  "return 'true' if an update has been performed on a record" in new Setup {

    override val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    override val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    await(for {
      _                   <- repository.set(eori, notificationEmail)
      currentNotification <- repository.get(eori)
      _                   <- repository.findAndUpdate(eori, undeliverableInformation)
      newNotification     <- repository.get(eori)
      _                   <- dropData()
    } yield {
      currentNotification mustBe Some(notificationEmail)
      newNotification mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
    })
  }

  "return 'false' if no update performed" in new Setup {
    val otherEori = "someEori"

    override val eori = "UnknownEori"

    override val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    override val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    await(for {
      _      <- repository.set(otherEori, notificationEmail)
      result <- repository.findAndUpdate(eori, undeliverableInformation)
      _       = result mustBe None
      record <- repository.get(eori)
      _      <- dropData()
    } yield record mustBe None)
  }

  "remove the undeliverable object when setting a new email address" in new Setup {

    override val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    override val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    await(for {
      _            <- repository.set(eori, notificationEmail)
      _            <- repository.findAndUpdate(eori, undeliverableInformation)
      firstResult  <- repository.get(eori)
      _            <- repository.set(eori, notificationEmail)
      secondResult <- repository.get(eori)
      _            <- dropData()
    } yield {
      firstResult mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
      secondResult mustBe Some(notificationEmail)
    })
  }

  "nextJob returns a job that still needs to be processed" in new Setup {

    override val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    override val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    val undeliverableInformationMongo: UndeliverableInformationMongo =
      UndeliverableInformationMongo(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )

    val undeliverableNotificationEmail: NotificationEmail =
      NotificationEmail("some@email.com", dateTime, Some(undeliverableInformation))

    val undeliverableNotificationEmailMongo: NotificationEmailMongo =
      NotificationEmailMongo("some@email.com", dateTime, Some(undeliverableInformationMongo))

    await(
      for {
        _      <- repository.set(eori, notificationEmail)
        _      <- repository.set(eori, undeliverableNotificationEmail)
        result <- repository.nextJobs
        _      <- dropData()
      } yield result mustBe List(undeliverableNotificationEmailMongo)
    )
  }

  "reset processing will make the next job pick the data up again" in new Setup {
    override val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    override val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    val undeliverableInformationMongo: UndeliverableInformationMongo =
      UndeliverableInformationMongo(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )

    val undeliverableNotificationEmail: NotificationEmail =
      NotificationEmail("some@email.com", dateTime, Some(undeliverableInformation))

    val undeliverableNotificationEmailMongo: NotificationEmailMongo =
      NotificationEmailMongo("some@email.com", dateTime, Some(undeliverableInformationMongo))

    await(
      for {
        _       <- repository.set(eori, undeliverableNotificationEmail)
        result1 <- repository.nextJobs
        result2 <- repository.nextJobs
        _       <- repository.resetProcessing(eori)
        result3 <- repository.nextJobs
        _       <- dropData()
      } yield {
        result1 mustBe Seq(undeliverableNotificationEmailMongo)
        result2 mustBe Seq.empty
        result3 mustBe
          Seq(
            undeliverableNotificationEmailMongo.copy(
              undeliverable = Some(undeliverableInformationMongo.copy(attempts = 1))
            )
          )
      }
    )
  }

  "mark as successful will ensure that the next job will not pick the data up again" in new Setup {

    override val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    override val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    val undeliverableNotificationEmail: NotificationEmail =
      NotificationEmail("some@email.com", dateTime, Some(undeliverableInformation))

    await(
      for {
        _      <- repository.set(eori, undeliverableNotificationEmail)
        _      <- repository.markAsSuccessful(eori)
        result <- repository.nextJobs
      } yield result mustBe Seq.empty
    )
  }

  trait Setup {
    val eori                                 = "SomeEori"
    val dateTime: LocalDateTime              = LocalDateTime.now()
    val notificationEmail: NotificationEmail = NotificationEmail("some@email.com", dateTime, None)
    val code                                 = 12

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(code),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori",
      Some("sdds")
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        dateTime,
        undeliverableInformationEvent
      )

    val mockHttpClient: HttpClientV2   = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    implicit val hc: HeaderCarrier     = HeaderCarrier()

    val app: Application = application
      .overrides(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[RequestBuilder].toInstance(requestBuilder)
      )
      .build()

    val repository: DefaultEmailRepository = app.injector.instanceOf[DefaultEmailRepository]

    val repoWithUnacknowledgedWrite: EmailRepositoryWithUnacknowledgedWrite =
      app.injector.instanceOf[EmailRepositoryWithUnacknowledgedWrite]

    def dropData(): Future[Unit] =
      repository.collection.drop().toFuture().map(_ => ())
  }
}

@Singleton
class EmailRepositoryWithUnacknowledgedWrite @Inject() ()(mongoComponent: MongoComponent, config: AppConfig)
    extends DefaultEmailRepository(mongoComponent, config) {

  override lazy val collection: MongoCollection[NotificationEmailMongo] =
    CollectionFactory
      .collection(mongoComponent.database, collectionName, domainFormat, Seq.empty)
      .withWriteConcern(WriteConcern.UNACKNOWLEDGED)
}
