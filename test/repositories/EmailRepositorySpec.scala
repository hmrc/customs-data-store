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

import models.repositories.NoEmailDocumentsUpdated
import org.joda.time.DateTime
import play.api.Application
import models.{NotificationEmail, UndeliverableInformation, UndeliverableInformationEvent}
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailRepositorySpec extends SpecBase {

  private val app: Application = application.build()
  val repository: DefaultEmailRepository = app.injector.instanceOf[DefaultEmailRepository]

  def dropData(): Future[Unit] = {
    repository.collection.drop().toFuture().map(_ => ())
  }

  "return 'true' if an update has been performed on a record" in {
    val eori = "SomeEori"
    val notificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori"
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        DateTime.now(),
        undeliverableInformationEvent
      )

    await(for {
      _ <- repository.set(eori, notificationEmail)
      currentNotification <- repository.get(eori)
      _ <- repository.update(eori, undeliverableInformation)
      newNotification <- repository.get(eori)
      _ <- dropData()
    } yield {
      currentNotification mustBe Some(notificationEmail)
      newNotification mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
    })
  }

  "return 'false' if no update performed" in {
    val eori = "UnknownEori"
    val otherEori = "someEori"
    val notificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori"
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        DateTime.now(),
        undeliverableInformationEvent
      )
    await(for {
      _ <- repository.set(otherEori, notificationEmail)
      result <- repository.update(eori, undeliverableInformation)
      _ = result mustBe NoEmailDocumentsUpdated
      record <- repository.get(eori)
      _ <- dropData()
    } yield {
      record mustBe None
    })
  }

  "remove the undeliverable object when setting a new email address" in {
    val eori = "someEori"
    val notificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori"
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        DateTime.now(),
        undeliverableInformationEvent
      )
    await(for {
      _ <- repository.set(eori, notificationEmail)
      _ <- repository.update(eori, undeliverableInformation)
      firstResult <- repository.get(eori)
      _ <- repository.set(eori, notificationEmail)
      secondResult <- repository.get(eori)
      _ <- dropData()
    } yield {
      firstResult mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
      secondResult mustBe Some(notificationEmail)
    })
  }

  "nextJob returns a job that still needs to be processed" in {
    val eori = "someEori"
    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori"
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        DateTime.now(),
        undeliverableInformationEvent
      )
    val deliverableNotificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)
    val undeliverableNotificationEmail = NotificationEmail("some@email.com", DateTime.now(), Some(undeliverableInformation))
    await(
      for {
        _ <- repository.set(eori, deliverableNotificationEmail)
        _ <- repository.set(eori, undeliverableNotificationEmail)
        result <- repository.nextJob
        _ <- dropData()
      } yield {
        result mustBe Some(undeliverableNotificationEmail)
      }
    )
  }

  "reset processing will make the next job pick the data up again" in {
    val eori = "someEori"
    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori"
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        DateTime.now(),
        undeliverableInformationEvent
      )
    val undeliverableNotificationEmail = NotificationEmail("some@email.com", DateTime.now(), Some(undeliverableInformation))

    await(
      for {
        _ <- repository.set(eori, undeliverableNotificationEmail)
        result1 <- repository.nextJob
        result2 <- repository.nextJob
        _ <- repository.resetProcessing(eori)
        result3 <- repository.nextJob
        _ <- dropData()
      } yield {
        result1 mustBe Some(undeliverableNotificationEmail)
        result2 mustBe None
        result3 mustBe Some(undeliverableNotificationEmail)
      }
    )
  }

  "mark as successful will ensure that the next job will not pick the data up again" in {
    val eori = "someEori"
    val undeliverableInformationEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(
      "some-id",
      "some event",
      "some@email.com",
      "detected",
      Some(12),
      Some("unknown reason"),
      s"HMRC-CUS-ORG~EORINumber~$eori"
    )

    val undeliverableInformation: UndeliverableInformation =
      UndeliverableInformation(
        "some-subject",
        "some-event-id",
        "some-group-id",
        DateTime.now(),
        undeliverableInformationEvent
      )
    val undeliverableNotificationEmail = NotificationEmail("some@email.com", DateTime.now(), Some(undeliverableInformation))

    await(
      for {
        _ <- repository.set(eori, undeliverableNotificationEmail)
        _ <- repository.markAsSuccessful(eori)
        result <- repository.nextJob
      } yield {
        result mustBe None
      }
    )
  }
}
