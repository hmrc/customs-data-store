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

import com.mongodb.client.model.Indexes.ascending
import models.{XiEoriAddressInformation, XiEoriInformation}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Configuration
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.ToSingleObservablePublisher

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultXiEoriInformationRepository @Inject() (config: Configuration, mongoComponent: MongoComponent)(implicit
  executionContext: ExecutionContext
) extends PlayMongoRepository[XiEoriInformationMongo](
      collectionName = "xieori-information",
      mongoComponent = mongoComponent,
      domainFormat = XiEoriInformationMongo.format,
      indexes = Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("xieori-information-last-updated-index")
            .expireAfter(config.get[Long]("mongodb.timeToLiveInHoursXieoriInformation"), TimeUnit.HOURS)
        )
      )
    )
    with XiEoriInformationRepository {

  override def get(id: String): Future[Option[XiEoriInformation]] =
    collection
      .find(equal("_id", id))
      .toSingle()
      .toFutureOption()
      .map(_.map(_.toXiEoriInformation))

  override def set(id: String, xiEoriInformation: XiEoriInformation): Future[Unit] =
    collection
      .replaceOne(
        equal("_id", id),
        XiEoriInformationMongo(
          xiEoriInformation.xiEori,
          xiEoriInformation.consent,
          xiEoriInformation.address,
          LocalDateTime.now()
        ),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
}

trait XiEoriInformationRepository {
  def get(id: String): Future[Option[XiEoriInformation]]

  def set(id: String, xieoriInformation: XiEoriInformation): Future[Unit]
}

case class XiEoriInformationMongo(
  xiEori: String,
  consent: String,
  address: XiEoriAddressInformation,
  lastUpdated: LocalDateTime
) {
  def toXiEoriInformation: XiEoriInformation = XiEoriInformation(xiEori, consent, address)
}

object XiEoriInformationMongo {
  implicit val mongoFormat: Format[LocalDateTime]      = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[XiEoriInformationMongo] = Json.format[XiEoriInformationMongo]
}
