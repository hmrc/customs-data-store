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
import models.EoriPeriod
import org.mongodb.scala.MongoCollection
import org.scalatest.Assertion
import play.api.{Application, Configuration}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.CollectionFactory
import utils.SpecBase

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HistoricEoriRepositorySpec extends SpecBase {

  "HistoricEoriRepository" should {

    "not retrieve trader information when the store is empty" in new Setup {
      await(for {
        eoris1 <- repository.get(period1.eori)
        _ <- toFuture(eoris1.swap.getOrElse(FailedToRetrieveHistoricEori) mustBe FailedToRetrieveHistoricEori)
        eoris2 <- repository.get(period2.eori)
        _ <- toFuture(eoris2.swap.getOrElse(FailedToRetrieveHistoricEori) mustBe FailedToRetrieveHistoricEori)
      } yield {})
    }

    "fail to UpdateHistoricEori" in new Setup {
      val eoriHistory: Seq[EoriPeriod] = Seq(period1, period2)

      repoWithUnacknowledgedWrite.set(eoriHistory).map {
        result => result mustBe FailedToUpdateHistoricEori
      }
    }

    "retrieve eori history with any of its historic eoris" in new Setup {
      val history: EoriHistory = EoriHistory(Seq(period1, period2), LocalDateTime.now)

      await(for {
        _ <- repository.set(Seq(period1, period2))
        _ <- repository.set(Seq(period4, period5))
        t1 <- repository.get(period1.eori)
        t2 <- repository.get(period2.eori)
        _ <- toFuture(t1.getOrElse(Seq()) mustBe history.eoriPeriods)
        _ <- toFuture(t2.getOrElse(Seq()) mustBe history.eoriPeriods)
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield ())
    }

    "retrieve trader information by the latest historic eori" in new Setup {
      await(for {
        _ <- repository.set(Seq(period1, period3))
        eoris <- repository.get(period1.eori)
        _ <- toFuture(eoris.getOrElse(Seq()) mustBe Seq(period1, period3))
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield {})
    }

    "retrieve trader information by the earliest historic eori" in new Setup {
      await(for {
        _ <- repository.set(Seq(period1, period3))
        eoris <- repository.get(period3.eori)
        _ <- toFuture(eoris.getOrElse(Seq()) mustBe Seq(period1, period3))
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield {})
    }

    "not retrieve trader information for eoris that are not historic eoris" in new Setup {
      await(for {
        eoris1 <- repository.get(period4.eori)
        _ <- toFuture(eoris1.swap.getOrElse(FailedToRetrieveHistoricEori) mustBe FailedToRetrieveHistoricEori)
        eoris2 <- repository.get(period5.eori)
        _ <- toFuture(eoris2.swap.getOrElse(FailedToRetrieveHistoricEori) mustBe FailedToRetrieveHistoricEori)
        _ <- repository.collection.drop().toFuture().map(_ => ())
      } yield {})
    }
  }

  trait Setup {
    val eori1: String = "EORI00000001"
    val eori2: String = "EORI00000002"
    val eori3: String = "EORI00000003"
    val eori4: String = "EORI00000004"
    val eori5: String = "EORI00000005"

    val period1: EoriPeriod = EoriPeriod(eori1, Some("2001-01-20T00:00:00Z"), None)
    val period2: EoriPeriod = EoriPeriod(eori2, Some("2002-01-20T00:00:00Z"), None)
    val period3: EoriPeriod = EoriPeriod(eori3, Some("2003-01-20T00:00:00Z"), None)
    val period4: EoriPeriod = EoriPeriod(eori4, Some("2005-01-20T00:00:00Z"), None)
    val period5: EoriPeriod = EoriPeriod(eori5, Some("2006-01-20T00:00:00Z"), None)

    def toFuture(condition: Assertion): Future[Assertion] = Future.successful(condition)

    val app: Application = application.build()

    val repoWithUnacknowledgedWrite: DefaultHistoricEoriRepoWithUnacknowledgedWrite =
      app.injector.instanceOf[DefaultHistoricEoriRepoWithUnacknowledgedWrite]

    val repository: DefaultHistoricEoriRepository = app.injector.instanceOf[DefaultHistoricEoriRepository]
  }
}

@Singleton
class DefaultHistoricEoriRepoWithUnacknowledgedWrite @Inject()()(mongoComponent: MongoComponent,
                                                                 config: Configuration)
  extends DefaultHistoricEoriRepository()(mongoComponent, config) {

  override lazy val collection: MongoCollection[EoriHistory] =
    CollectionFactory.collection(
      mongoComponent.database,
      collectionName,
      domainFormat,
      Seq.empty).withWriteConcern(WriteConcern.UNACKNOWLEDGED)
}
