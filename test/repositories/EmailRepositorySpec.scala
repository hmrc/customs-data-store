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

import models.repositories.{NotificationEmailMongo, UndeliverableInformationMongo}
import models.{NotificationEmail, UndeliverableInformation, UndeliverableInformationEvent}
import org.joda.time.DateTime
import play.api.Application
import utils.SpecBase
import models.repositories.{SuccessfulEmail, FailedToRetrieveEmail}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

class EmailRepositorySpec extends SpecBase {

  private val app: Application = application.build()
  val repository: DefaultEmailRepository = app.injector.instanceOf[DefaultEmailRepository]

  def dropData(): Future[Unit] = {
    repository.collection.drop().toFuture().map(_ => ())
  }

  "successfully retrive email from repository" in new Setup {
    val notificationEmail = NotificationEmail("123", DateTime.now, Some(undeliverableInformation))
    repository.set(eori, notificationEmail).map {
      result => result mustBe SuccessfulEmail
    }
  }

  "fail to retrieve email from repository after setting bad email" in new Setup {
    val mockEmailRepository = mock[DefaultEmailRepository]
    when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(FailedToRetrieveEmail))
    val notificationEmail = NotificationEmail("123", DateTime.now, Some(undeliverableInformation))

    mockEmailRepository.set(eori, notificationEmail).map {
      result => result mustBe FailedToRetrieveEmail
    }
  }

  "return 'true' if an update has been performed on a record" in new Setup {
    val notificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    await(for {
      _ <- repository.set(eori, notificationEmail)
      currentNotification <- repository.get(eori)
      _ <- repository.findAndUpdate(eori, undeliverableInformation)
      newNotification <- repository.get(eori)
      _ <- dropData()
    } yield {
      currentNotification mustBe Some(notificationEmail)
      newNotification mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
    })
  }

  "return 'false' if no update performed" in new Setup {
    val unknownEori = "UnknownEori"
    val notificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    for {
      _ <- repository.set(eori, notificationEmail)
      result <- repository.findAndUpdate(unknownEori, undeliverableInformation)
      _ = result.map(notif => notif mustBe None)
      record <- repository.get(unknownEori)
      _ <- dropData()
    } yield {
      record mustBe None
    }
  }

  "remove the undeliverable object when setting a new email address" in new Setup {
    val notificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    for {
      _ <- repository.set(eori, notificationEmail)
      _ <- repository.findAndUpdate(eori, undeliverableInformation)
      firstResult <- repository.get(eori)
      _ <- repository.set(eori, notificationEmail)
      secondResult <- repository.get(eori)
      _ <- dropData()
    } yield {
      firstResult mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
      secondResult mustBe Some(notificationEmail)
    }
  }

  "nextJob returns a job that still needs to be processed" in new Setup {

    val deliverableNotificationEmail = NotificationEmail("some@email.com", detectedDate, None)

    val undeliverableNotificationEmail = NotificationEmail(
      "some@email.com", detectedDate, Some(undeliverableInformation))

    val undeliverableNotificationEmailMongo = NotificationEmailMongo(
      "some@email.com", detectedDate, Some(undeliverableInformationMongo))
    await(
      for {
        _ <- repository.set(eori, deliverableNotificationEmail)
        _ <- repository.set(eori, undeliverableNotificationEmail)
        result <- repository.nextJobs
        _ <- dropData()
      } yield {
        result mustBe List(undeliverableNotificationEmailMongo)
      }
    )
  }

  "reset processing will make the next job pick the data up again" in new Setup {

    val undeliverableNotificationEmail = NotificationEmail(
      "some@email.com", DateTime.now(), Some(undeliverableInformation))

    val undeliverableNotificationEmailMongo = NotificationEmailMongo(
      "some@email.com", DateTime.now(), Some(undeliverableInformationMongo))

    await(
      for {
        _ <- repository.set(eori, undeliverableNotificationEmail)
        result1 <- repository.nextJobs
        result2 <- repository.nextJobs
        _ <- repository.resetProcessing(eori)
        result3 <- repository.nextJobs
        _ <- dropData()
      } yield {
        result1 mustBe Seq(undeliverableNotificationEmailMongo)
        result2 mustBe Seq.empty
        result3 mustBe Seq(undeliverableNotificationEmailMongo.copy(undeliverable = Some(undeliverableInformationMongo.copy(attempts = 1))))
      }
    )
  }

  "mark as successful will ensure that the next job will not pick the data up again" in new Setup {

    val undeliverableNotificationEmail = NotificationEmail(
      "some@email.com", DateTime.now(), Some(undeliverableInformation))

      for {
        _ <- repository.set(eori, undeliverableNotificationEmail)
        _ <- repository.markAsSuccessful(eori)
        result <- repository.nextJobs
      } yield {
        result mustBe Seq.empty
      }
  }

  trait Setup {
    val detectedDate: DateTime = DateTime.now()
    val eori = "SomeEori"

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      detectedDate.toString(),
      Some(12),
      Some("unknown reason"),
      s"HMRC-cus-ORG~EORINUMBER~$eori",
      Some("sdds")
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        detectedDate,
        undeliverableInformationEvent
      )

    val undeliverableInformationMongo: UndeliverableInformationMongo =
      UndeliverableInformationMongo(
        "some-subject",
        "some-event-id",
        "some-group-id",
        detectedDate,
        undeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )
  }
}
