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
import models.{AddressInformation, CompanyInformation}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Configuration
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.ToSingleObservablePublisher

@Singleton
class DefaultCompanyInformationRepository @Inject() (config: Configuration, mongoComponent: MongoComponent)(implicit
  executionContext: ExecutionContext
) extends PlayMongoRepository[CompanyInformationMongo](
      collectionName = "business-information",
      mongoComponent = mongoComponent,
      domainFormat = CompanyInformationMongo.format,
      indexes = Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("business-information-last-updated-index")
            .expireAfter(config.get[Long]("mongodb.timeToLiveInHoursBusinessInformation"), TimeUnit.HOURS)
        )
      )
    )
    with CompanyInformationRepository {

  override def get(id: String): Future[Option[CompanyInformation]] =
    collection
      .find(equal("_id", id))
      .toSingle()
      .toFutureOption()
      .map(_.map(_.toCompanyInformation))

  override def set(id: String, companyInformation: CompanyInformation): Future[Unit] =
    collection
      .replaceOne(
        equal("_id", id),
        CompanyInformationMongo(
          companyInformation.name,
          companyInformation.consent,
          companyInformation.address,
          LocalDateTime.now()
        ),
        ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
}

trait CompanyInformationRepository {
  def get(id: String): Future[Option[CompanyInformation]]

  def set(id: String, businessInformation: CompanyInformation): Future[Unit]
}

case class CompanyInformationMongo(
  name: String,
  consent: String,
  address: AddressInformation,
  lastUpdated: LocalDateTime
) {
  def toCompanyInformation: CompanyInformation = CompanyInformation(name, consent, address)
}

trait MongoJavatimeFormats {
  outer =>
  final val localDateTimeReads: Reads[LocalDateTime] =
    Reads
      .at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  final val localDateTimeWrites: Writes[LocalDateTime] =
    Writes
      .at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  final val localDateTimeFormat: Format[LocalDateTime] =
    Format(localDateTimeReads, localDateTimeWrites)

  trait Implicits {
    implicit val jatLocalDateTimeFormat: Format[LocalDateTime] = outer.localDateTimeFormat
  }

  object Implicits extends Implicits
}
object MongoJavatimeFormats extends MongoJavatimeFormats

object CompanyInformationMongo {
  implicit val mongoFormat: Format[LocalDateTime]       = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[CompanyInformationMongo] = Json.format[CompanyInformationMongo]
}
