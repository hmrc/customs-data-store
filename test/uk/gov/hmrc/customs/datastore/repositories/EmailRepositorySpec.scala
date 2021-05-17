package uk.gov.hmrc.customs.datastore.repositories

import org.joda.time.DateTime
import play.api.Application
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.customs.datastore.domain.{NotificationEmail, UndeliverableInformation}
import uk.gov.hmrc.customs.datastore.utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailRepositorySpec extends SpecBase {

  private val app: Application = application.build()
  val repository = app.injector.instanceOf[EmailRepository]
  val reactiveMongoAPi = app.injector.instanceOf[ReactiveMongoApi]


  def dropData(): Future[Unit] = {
    for {
      col <- reactiveMongoAPi.database.map(_.collection[JSONCollection]("email-verification"))
      _ <- col.drop(false)
    } yield ()
  }

  "return 'true' if an update has been performed on a record" in {
    val eori = "SomeEori"
    val notificationEmail = NotificationEmail(Some("some@email.com"), Some(DateTime.now()))
    val undeliverableInformation = UndeliverableInformation("EORINumber", eori, "some2@email.com", "some event", DateTime.now(), None, None)

    await(for {
      _ <- repository.set(eori, notificationEmail)
      currentNotification <- repository.get(eori)
      _ = currentNotification mustBe Some(notificationEmail)
      _ <- repository.update(undeliverableInformation)
      newNotification <- repository.get(eori)
      _ = newNotification mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
      _ <- dropData()
    } yield {})
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
      _ = record mustBe None
      _ <- dropData()
    } yield {})
  }

  "remove the undeliverable object when setting a new email address" in {
    val eori = "someEori"
    val notificationEmail = NotificationEmail(Some("some@email.com"), Some(DateTime.now()))
    val undeliverableInformation = UndeliverableInformation("EORINumber", eori, "some2@email.com", "some event", DateTime.now(), None, None)

    await(for {
      _ <- repository.set(eori, notificationEmail)
      _ <- repository.update(undeliverableInformation)
      firstResult <- repository.get(eori)
      _ = firstResult mustBe Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
      _ <- repository.set(eori, notificationEmail)
      secondResult <- repository.get(eori)
      _ = secondResult mustBe Some(notificationEmail)
      _ <- dropData()
    } yield {})
  }
}
