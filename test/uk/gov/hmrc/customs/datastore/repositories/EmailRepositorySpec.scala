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

import org.joda.time.DateTime
import play.api.Application
import uk.gov.hmrc.customs.datastore.domain.{NotificationEmail, UndeliverableInformation}
import uk.gov.hmrc.customs.datastore.utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailRepositorySpec extends SpecBase {

  private val app: Application = application.build()
  val repository = app.injector.instanceOf[DefaultEmailRepository]

  def dropData(): Future[Unit] = {
    repository.collection.drop().toFuture().map(_ => ())
  }

  "return 'true' if an update has been performed on a record" in {
    val eori = "SomeEori"
    val notificationEmail = NotificationEmail(Some("some@email.com"), Some(DateTime.now()))
    val undeliverableInformation = UndeliverableInformation("EORINumber", eori, "some2@email.com", "some event", DateTime.now(), None, None)

    await(for {
      _ <- repository.set(eori, notificationEmail)
      currentNotification <- repository.get(eori)
      _ <- repository.update(undeliverableInformation)
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
    val notificationEmail = NotificationEmail(Some("some@email.com"), Some(DateTime.now()))
    val undeliverableInformation = UndeliverableInformation("EORINumber", eori, "some2@email.com", "some event", DateTime.now(), None, None)

    await(for {
      _ <- repository.set(otherEori, notificationEmail)
      result <- repository.update(undeliverableInformation)
      _ = result mustBe false
      record <- repository.get(eori)
      _ <- dropData()
    } yield {
      record mustBe None
    })
  }

  "remove the undeliverable object when setting a new email address" in {
    val eori = "someEori"
    val notificationEmail = NotificationEmail(Some("some@email.com"), Some(DateTime.now()))
    val undeliverableInformation = UndeliverableInformation("EORINumber", eori, "some2@email.com", "some event", DateTime.now(), None, None)

    await(for {
      _ <- repository.set(eori, notificationEmail)
      _ <- repository.update(undeliverableInformation)
      firstResult <- repository.get(eori)
      _ <- repository.set(eori, notificationEmail)
      secondResult <- repository.get(eori)
      _ <- dropData()
    } yield {
      firstResult mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
      secondResult mustBe Some(notificationEmail)
    })
  }
}
