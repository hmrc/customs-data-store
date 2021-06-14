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

import com.mongodb.client.model.Indexes.ascending
import org.mongodb.scala.model.Filters.{equal, in}
import org.mongodb.scala.model.{IndexModel, IndexOptions, UpdateOptions, Updates}
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, Reads, __}
import models.EoriPeriod
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultHistoricEoriRepository @Inject()()(
  mongoComponent: MongoComponent,
  config: Configuration
)(implicit executionContext: ExecutionContext)
  extends PlayMongoRepository[EoriHistory](
    collectionName = "historic-eoris",
    mongoComponent = mongoComponent,
    domainFormat = EoriHistory.format,
    indexes = Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions().name("historic-eoris-last-updated-index")
          .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
      ))
  ) with HistoricEoriRepository {

  override def get(id: String): Future[Option[Seq[EoriPeriod]]] =
    collection.find(equal("eoriHistory.eori", id)).toFuture().map(_.headOption.map(_.eoriPeriods))

  override def set(eoriHistory: Seq[EoriPeriod]): Future[Boolean] = {
    val query = in("eoriHistory.eori", eoriHistory.map(_.eori): _*)

    val update = Updates.combine(
      Updates.set("eoriHistory", eoriHistory.map(Codecs.toBson(_))),
      Updates.set("lastUpdated", LocalDateTime.now())
    )

    collection.updateMany(query, update, UpdateOptions().upsert(true)).toFuture().map(v => v.wasAcknowledged())
  }
}

trait HistoricEoriRepository {
  def get(id: String): Future[Option[Seq[EoriPeriod]]]
  def set(eoriHistory: Seq[EoriPeriod]): Future[Boolean]
}

case class EoriHistory(eoriPeriods: Seq[EoriPeriod], lastUpdated: LocalDateTime = LocalDateTime.now)

object EoriHistory {
  implicit lazy val reads: Reads[EoriHistory] = {
    (
      (__ \ "eoriHistory").read[Seq[EoriPeriod]] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.localDateTimeReads)
      ) (EoriHistory.apply _)
  }
  implicit val format: Format[EoriHistory] = Format(reads, Json.writes[EoriHistory])
}
