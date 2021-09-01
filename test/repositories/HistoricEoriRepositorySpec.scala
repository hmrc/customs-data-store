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

import org.scalatest.Assertion
import play.api.Application
import models.EoriPeriod
import utils.SpecBase

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HistoricEoriRepositorySpec extends SpecBase {

  private val app: Application = application.build()
  val repository = app.injector.instanceOf[DefaultHistoricEoriRepository]

  val eori1: String = "EORI00000001"
  val eori2: String = "EORI00000002"
  val eori3: String = "EORI00000003"
  val eori4: String = "EORI00000004"
  val eori5: String = "EORI00000005"

  val period1 = EoriPeriod(eori1, Some("2001-01-20T00:00:00Z"), None)
  val period2 = EoriPeriod(eori2, Some("2002-01-20T00:00:00Z"), None)
  val period3 = EoriPeriod(eori3, Some("2003-01-20T00:00:00Z"), None)
  val period4 = EoriPeriod(eori4, Some("2005-01-20T00:00:00Z"), None)
  val period5 = EoriPeriod(eori5, Some("2006-01-20T00:00:00Z"), None)

  def toFuture(condition: Assertion) = Future.successful(condition)

  "HistoricEoriRepository" should {

    "not retrieve trader information when the store is empty" in {
      await(for {
        eoris1 <- repository.get(period1.eori)
        _ <- toFuture(eoris1.left.get mustBe FailedToRetrieveHistoricEori)
        eoris2 <- repository.get(period2.eori)
        _ <- toFuture(eoris2.left.get mustBe FailedToRetrieveHistoricEori)
      } yield {})
    }

    "retrieve eori history with any of its historic eoris" in {
      val history = EoriHistory(Seq(period1, period2), LocalDateTime.now)
      await(for {
        _ <- repository.set(Seq(period1, period2))
        _ <- repository.set(Seq(period4, period5))
        t1 <- repository.get(period1.eori)
        t2 <- repository.get(period2.eori)
        _ <- toFuture(t1.right.get mustBe history.eoriPeriods)
        _ <- toFuture(t2.right.get mustBe history.eoriPeriods)
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield ())
    }

    "retrieve trader information by the latest historic eori" in {
      await(for {
        _ <- repository.set(Seq(period1, period3))
        eoris <- repository.get(period1.eori)
        _ <- toFuture(eoris.right.get mustBe Seq(period1, period3))
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield {})
    }

    "retrieve trader information by the earliest historic eori" in {
      await(for {
        _ <- repository.set(Seq(period1, period3))
        eoris <- repository.get(period3.eori)
        _ <- toFuture(eoris.right.get mustBe Seq(period1, period3))
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield {})
    }

    "not retrieve trader information for eoris that are not historic eoris" in {
      await(for {
        eoris1 <- repository.get(period4.eori)
        _ <- toFuture(eoris1.left.get mustBe FailedToRetrieveHistoricEori)
        eoris2 <- repository.get(period5.eori)
        _ <- toFuture(eoris2.left.get mustBe FailedToRetrieveHistoricEori)
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield {})
    }
  }
}