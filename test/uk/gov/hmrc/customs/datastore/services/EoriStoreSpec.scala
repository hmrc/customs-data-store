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

package uk.gov.hmrc.customs.datastore.services


import org.scalatest.Assertion
import play.api.{Application, inject}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.customs.datastore.domain._
import uk.gov.hmrc.customs.datastore.utils.SpecBase
import uk.gov.hmrc.mongo.MongoConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EoriStoreSpec extends SpecBase {

  override def beforeEach: Unit = {
    await(eoriStore.removeAll())
  }

  val reactiveMongo = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }

  private val app: Application = application.overrides(
    inject.bind[ReactiveMongoComponent].toInstance(reactiveMongo)
  ).build()

  val eoriStore = app.injector.instanceOf[EoriStore]

  val verifiedEmail = Some(NotificationEmail(Some("test@email.uk"), Some("timestamp")))
  val unverifiedEmailNoTimestamp = Some(NotificationEmail(Some("test@email.uk"), None))
  val unverifiedEmailNoAddress = Some(NotificationEmail(None, Some("timestamp")))
  val unverifiedEmailNoAddressNoTimeStamp: Option[NotificationEmail] = Some(NotificationEmail(None, None))
  val noEmail = None


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

    "not retrieve trader information when the store is empty" in {
      await(for {
        eoris1 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris1 mustBe None)
        eoris2 <- eoriStore.findByEori(period2.eori)
        _ <- toFuture(eoris2 mustBe None)
      } yield {})
    }

    "retrieve trader information with any of its historic eoris" in {
      val trader1 = TraderData(eoriHistory = Seq(period1, period2), notificationEmail = noEmail)
      await(for {
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period2))
        _ <- eoriStore.updateHistoricEoris(Seq(period5, period6))
        t1 <- eoriStore.findByEori(period1.eori)
        t2 <- eoriStore.findByEori(period2.eori)
        _ <- toFuture(t1 mustBe Some(trader1))
        _ <- toFuture(t2 mustBe Some(trader1))
      } yield ())
    }

    "retrieve trader information by the latest historic eori" in {
      await(for {
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period3))
        eoris <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris mustBe Some(TraderData(Seq(period1, period3), noEmail)))
      } yield {})
    }

    "retrieve trader information by the earliest historic eori" in {
      await(for {
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period3))
        eoris <- eoriStore.findByEori(period3.eori)
        _ <- toFuture(eoris mustBe Some(TraderData(Seq(period1, period3), noEmail)))
      } yield {})
    }

    "not retrieve trader information for eoris that are not historic eoris" in {
      await(for {
        eoris1 <- eoriStore.findByEori(period5.eori)
        _ <- toFuture(eoris1 mustBe None)
        eoris2 <- eoriStore.findByEori(period6.eori)
        _ <- toFuture(eoris2 mustBe None)
      } yield {})
    }

    "not return trader data with an email address when an historic eoris is updated with an unverified email" in {
      val noEmail = None
      await(for {
        _ <- eoriStore.upsertByEori(EoriPeriod(period1.eori, None, None), unverifiedEmailNoAddress)
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period2))
        _ <- eoriStore.updateHistoricEoris(Seq(period5, period6))
        eoris1 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris1 mustBe Some(TraderData(Seq(period1, period2), noEmail)))
        eoris2 <- eoriStore.findByEori(period2.eori)
        _ <- toFuture(eoris2 mustBe Some(TraderData(Seq(period1, period2), noEmail)))
      } yield ())
    }

    "return trader data with an email address when an historic eoris is updated with a verified email" in {
      await(for {
        _ <- eoriStore.upsertByEori(EoriPeriod(period1.eori, None,None), verifiedEmail)
        _ <- eoriStore.updateHistoricEoris(Seq(period1, period3))
        eoris3 <- eoriStore.findByEori(period1.eori)
        _ <- toFuture(eoris3 mustBe Some(TraderData(Seq(period1, period3), verifiedEmail)))
        eoris4 <- eoriStore.findByEori(period3.eori)
        _ <- toFuture(eoris4 mustBe Some(TraderData(Seq(period1, period3), verifiedEmail)))
      } yield ())
    }


    "upsertByEori" should {

      "insert eori" in {
        val eoriPeriod = EoriPeriod(eori1, None, None)
        val traderData1 = TraderData(Seq(EoriPeriod(eori1, None, None)), noEmail)

        await(for {
          _ <- eoriStore.upsertByEori(eoriPeriod, None)
          r1 <- eoriStore.findByEori(eori1)
          _ <- toFuture(r1.get mustBe traderData1)
        } yield ())
      }

      "insert eori with validFrom and validUntil " in {
        val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
        val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), noEmail)

        await(for {
          _ <- eoriStore.upsertByEori(eoriPeriod, None)
          r1 <- eoriStore.findByEori(eori1)
          _ <- toFuture(r1.get mustBe expected)
        } yield ())
      }

      "saveEoris methods should not remove any existing data" in {
        await(for {
          _ <- eoriStore.upsertByEori(EoriPeriod(eori1, None, None), verifiedEmail)
          r1 <- eoriStore.findByEori(eori1)
          _ <- toFuture(r1.get mustBe TraderData(Seq(EoriPeriod(eori1, None, None)), verifiedEmail))
          _ <- eoriStore.updateHistoricEoris(Seq(EoriPeriod(eori1,Some("from"),Some("to"))))
          r2 <- eoriStore.findByEori(eori1)
          _ <- toFuture(r2.get mustBe TraderData(Seq(EoriPeriod(eori1, Some("from"), Some("to"))), verifiedEmail))
        } yield ())
      }

      "upsertByEori methods should remove any existing data" in {
        await(for {
          _ <- eoriStore.updateHistoricEoris(Seq(EoriPeriod(eori1,Some("from1"),Some("to1")),EoriPeriod(eori2,Some("from2"),Some("to2"))))
          r2 <- eoriStore.findByEori(eori1)
          _ <- toFuture(r2.get mustBe TraderData(Seq(EoriPeriod(eori1, Some("from1"), Some("to1")),EoriPeriod(eori2, Some("from2"), Some("to2"))), noEmail))
          _ <- eoriStore.upsertByEori(EoriPeriod(eori1, None, None), verifiedEmail)
          r1 <- eoriStore.findByEori(eori1)
          _ <- toFuture(r1.get mustBe TraderData(Seq(EoriPeriod(eori1, Some("from1"), Some("to1")),EoriPeriod(eori2, Some("from2"), Some("to2"))), verifiedEmail))
        } yield ())
      }

    }

    "insert eori with verified notification email" in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), verifiedEmail)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, verifiedEmail)
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }

    "insert eori with no address as an unverified notification email" in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), noEmail)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, unverifiedEmailNoAddress)
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }

    "insert eori with no timestamp as an unverified notification email" in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), noEmail)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, unverifiedEmailNoTimestamp)
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }

    "insert eori with no address and no timestamp as an unverified notification email" in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val expected = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), noEmail)

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, unverifiedEmailNoAddressNoTimeStamp)
        r1 <- eoriStore.findByEori(eori1)
        _ <- toFuture(r1.get mustBe expected)
      } yield ())
    }


    "upsert the validFrom, validUntil, email and timestamp fields " in {
      val eoriPeriod = EoriPeriod(eori1, Some("date1"), Some("date2"))
      val expectedTraderDataAfterInsert = TraderData(Seq(EoriPeriod(eori1, Some("date1"), Some("date2"))), verifiedEmail)

      val updatedEoriPeriod = EoriPeriod(eori1, Some("date3"), Some("date4"))
      val updatedEmail = NotificationEmail(Some("updated@email.uk"), Some("timestamp2"))
      val expectedTraderDataAfterUpdate = TraderData(Seq(EoriPeriod(eori1, Some("date3"), Some("date4"))), Some(updatedEmail))

      await(for {
        _ <- eoriStore.upsertByEori(eoriPeriod, verifiedEmail)
        insertedTraderData <- eoriStore.findByEori(eori1)
        _ <- toFuture(insertedTraderData.get mustBe expectedTraderDataAfterInsert)
        _ <- eoriStore.upsertByEori(updatedEoriPeriod, Some(updatedEmail))
        updatedTraderData <- eoriStore.findByEori(eori1)
        _ <- toFuture(updatedTraderData.get mustBe expectedTraderDataAfterUpdate)
      } yield ())
    }

  }
}