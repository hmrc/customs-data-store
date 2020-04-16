/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.customs.datastore.services

import org.scalatest.{Assertion, BeforeAndAfterEach, MustMatchers, WordSpec}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.customs.datastore.config.AppConfig
import uk.gov.hmrc.customs.datastore.domain._
import uk.gov.hmrc.mongo.MongoConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EoriStoreSpec extends WordSpec with MustMatchers with MongoSpecSupport with DefaultAwaitTimeout with FutureAwaits with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    await(eoriStore.removeAll())
  }

  val reactiveMongo = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }
  val env = Environment.simple()
  val configuration = Configuration.load(env)
  val appConfig = new AppConfig(configuration, env)
  val eoriStore = new EoriStore(reactiveMongo,appConfig)

  val eori1: Eori = "EORI00000001"
  val eori2: Eori = "EORI00000002"
  val eori3: Eori = "EORI00000003"
  val eori4: Eori = "EORI00000004"
  val eori5: Eori = "EORI00000005"
  val eori6: Eori = "EORI00000006"

  val period1 = EoriPeriod(eori1, Some("2001-01-20T00:00:00Z"), None)
  val period2 = EoriPeriod(eori2, Some("2002-01-20T00:00:00Z"), None)
  val period3 = EoriPeriod(eori3, Some("2003-01-20T00:00:00Z"), None)
  val period4 = EoriPeriod(eori4, Some("2004-01-20T00:00:00Z"), None)
  val period5 = EoriPeriod(eori5, Some("2005-01-20T00:00:00Z"), None)
  val period6 = EoriPeriod(eori6, Some("2006-01-20T00:00:00Z"), None)

  def toFuture(condition: Assertion) = Future.successful(condition)

  "EoriStore" should {

    "retrieve trader information with any of its historic eoris" in {

      val trader1 = TraderData(eoriHistory = Seq(period1, period2), notificationEmail = None)
      val trader2 = TraderData(eoriHistory = Seq(period5, period6), notificationEmail = None)

      await(for {
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period2))
        _ <- eoriStore.updateHistoricEoris(Seq(period5, period6))
        t1 <- eoriStore.findByEori(period1.eori)
        t2 <- eoriStore.findByEori(period2.eori)
        _ <- toFuture(t1 mustBe Some(trader1))
        _ <- toFuture(t2 mustBe Some(trader1))
      } yield ())

    }

    //TODO: replace with simple scenario(s)/test(s) or rename test to reflect purpose
    "Complex email upsert test with empty database" in {
      await(for {
        eoris1 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris1 mustBe None)
        eoris2 <- eoriStore.findByEori(period2.eori)
        _ <- toFuture(eoris2 mustBe None)
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period3))
        eoris3 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris3 mustBe Some(TraderData(Seq(period1, period3), None)))
        _ <- toFuture(eoris3 mustBe Some(TraderData(Seq(period1, period3), None)))
        eoris4 <- eoriStore.findByEori(period3.eori)
        _ <- toFuture(eoris4 mustBe Some(TraderData(Seq(period1, period3), None)))
        _ <- toFuture(eoris4 mustBe Some(TraderData(Seq(period1, period3), None)))
        _ <- eoriStore.updateHistoricEoris(Seq(period3, period4))
        eoris5 <- eoriStore.findByEori(period3.eori)
        _ <- toFuture(eoris5 mustBe Some(TraderData(Seq(period3, period4), None)))
        _ <- toFuture(eoris5 mustBe Some(TraderData(Seq(period3, period4), None)))
        eoris6 <- eoriStore.findByEori(period4.eori)
        _ <- toFuture(eoris6 mustBe Some(TraderData(Seq(period3, period4), None)))
        _ <- toFuture(eoris6 mustBe Some(TraderData(Seq(period3, period4), None)))
        eoris7 <- eoriStore.findByEori(period5.eori)
        _ <- toFuture(eoris7 mustBe None)
        eoris8 <- eoriStore.findByEori(period6.eori)
        _ <- toFuture(eoris8 mustBe None)
      } yield {})

    }


    //TODO: replace with simple scenario(s)/test(s) or rename test to reflect purpose
    "Complex email upsert test with preloaded data" in {
      val noEmail = None
      await(for {
        _ <- eoriStore.upsertByEori(EoriPeriod(period1.eori, None,None),Some(NotificationEmail(Some("a.b@mail.com"),None)))
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period2))
        _ <- eoriStore.updateHistoricEoris(Seq(period5, period6))
        eoris1 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris1 mustBe Some(TraderData(Seq(period1, period2), noEmail)))
        eoris2 <- eoriStore.findByEori(period2.eori)
        _ <- toFuture(eoris2 mustBe Some(TraderData(Seq(period1, period2), noEmail)))
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period3))
        eoris3 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris3 mustBe Some(TraderData(Seq(period1, period3), noEmail)))
        eoris4 <- eoriStore.findByEori(period3.eori)
        _ <- toFuture(eoris4 mustBe Some(TraderData(Seq(period1, period3), noEmail)))
        _ <- eoriStore.updateHistoricEoris(Seq(period3, period4))
        eoris5 <- eoriStore.findByEori(period3.eori)
        _ <- toFuture(eoris5 mustBe Some(TraderData(Seq(period3, period4), noEmail)))
        eoris6 <- eoriStore.findByEori(period4.eori)
        _ <- toFuture(eoris6 mustBe Some(TraderData(Seq(period3, period4), noEmail)))
        eoris7 <- eoriStore.findByEori(period5.eori)
        _ <- toFuture(eoris7 mustBe Some(TraderData(Seq(period5, period6), noEmail)))
        eoris8 <- eoriStore.findByEori(period6.eori)
        _ <- toFuture(eoris8 mustBe Some(TraderData(Seq(period5, period6), noEmail)))
      } yield ())

    }

  "upsertByEori" should {

    "insert eori" in {
      val eoriPeriod = EoriPeriod(eori1, None, None)
      val traderData1 = TraderData(Seq(EoriPeriod(eori1, None, None)), None)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, None)
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe traderData1)
      } yield ())
    }

    "insert eori with validFrom and validUntil " in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), None)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, None)
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }

    "saveEoris methods should not remove any existing data" in {
      await(for {
        _ <- eoriStore.upsertByEori(EoriPeriod(eori1, None, None), Some(NotificationEmail(Some("test@email.uk"), Some("timestamp"))))
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe TraderData(Seq(EoriPeriod(eori1, None, None)), Some(NotificationEmail(Some("test@email.uk"), Some("timestamp")))))
        _ <- eoriStore.updateHistoricEoris(Seq(EoriPeriod(eori1,Some("from"),Some("to"))))
        r2 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r2.get mustBe TraderData(Seq(EoriPeriod(eori1, Some("from"), Some("to"))), Some(NotificationEmail(Some("test@email.uk"), Some("timestamp")))))
      } yield ())
    }

    "upsertByEori methods should remove any existing data" in {
      await(for {
        _ <- eoriStore.updateHistoricEoris(Seq(EoriPeriod(eori1,Some("from1"),Some("to1")),EoriPeriod(eori2,Some("from2"),Some("to2"))))
        r2 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r2.get mustBe TraderData(Seq(EoriPeriod(eori1, Some("from1"), Some("to1")),EoriPeriod(eori2, Some("from2"), Some("to2"))), None))
        _ <- eoriStore.upsertByEori(EoriPeriod(eori1, None, None), Some(NotificationEmail(Some("test@email.uk"), Some("timestamp"))))
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe TraderData(Seq(EoriPeriod(eori1, Some("from1"), Some("to1")),EoriPeriod(eori2, Some("from2"), Some("to2"))), Some(NotificationEmail(Some("test@email.uk"), Some("timestamp")))))
      } yield ())
    }

  }

    "insert eori with verified notification email" in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val email = NotificationEmail(Some("test@email.uk"), Some("timestamp"))
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), Some(NotificationEmail(Some("test@email.uk"), Some("timestamp"))))

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, Some(email))
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }

    "insert eori with unverified notification email" in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val email = NotificationEmail(Some("test@email.uk"), None)
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), None)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, Some(email))
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }

    "upsert the validFrom, validUntil, email and timestamp fields " in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val email = NotificationEmail(Some("original@email.uk"), Some("timestamp1"))
      val expectedTraderDataAfterInsert = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), Some(NotificationEmail(Some("original@email.uk"), Some("timestamp1"))))

      val updatedEoriPeriod = EoriPeriod(eori1, Some("date3"), Some("date4"))
      val updatedEmail = NotificationEmail(Some("updated@email.uk"), Some("timestamp2"))
      val expectedTraderDataAfterUpdate = TraderData(Seq(EoriPeriod(eori1, Some("date3"), Some("date4"))), Some(NotificationEmail(Some("updated@email.uk"), Some("timestamp2"))))

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, Some(email))
        insertedTraderData <- eoriStore.findByEori(eori1)
        _ <- toFuture(insertedTraderData.get mustBe expectedTraderDataAfterInsert)
        _ <- eoriStore.upsertByEori(updatedEoriPeriod, Some(updatedEmail))
        updatedTraderData <- eoriStore.findByEori(eori1)
        _ <- toFuture(updatedTraderData.get mustBe expectedTraderDataAfterUpdate)
      } yield ())
    }

  }


}
