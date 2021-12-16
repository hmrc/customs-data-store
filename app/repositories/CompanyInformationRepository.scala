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
import models.{AddressInformation, CompanyInformation}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Configuration
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultCompanyInformationRepository @Inject()(
                                                      config: Configuration,
                                                      mongoComponent: MongoComponent
                                                    )(implicit executionContext: ExecutionContext)
  extends PlayMongoRepository[CompanyInformationMongo](
    collectionName = "business-information",
    mongoComponent = mongoComponent,
    domainFormat = CompanyInformationMongo.format,
    indexes = Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions().name("business-information-last-updated-index")
          .expireAfter(config.get[Int]("mongodb.timeToLiveInHoursBusinessInformation"), TimeUnit.HOURS)
      ))
  ) with CompanyInformationRepository {

  override def get(id: String): Future[Option[CompanyInformation]] =
    collection
      .find(equal("_id", id))
      .toSingle()
      .toFutureOption()
      .map(_.map(_.toCompanyInformation))

  override def set(id: String, companyInformation: CompanyInformation): Future[Unit] =
    collection.replaceOne(
      equal("_id", id),
      CompanyInformationMongo(companyInformation.name, companyInformation.consent, companyInformation.address, LocalDateTime.now()),
      ReplaceOptions().upsert(true)
    ).toFuture().map(_ => ())
}

trait CompanyInformationRepository {
  def get(id: String): Future[Option[CompanyInformation]]

  def set(id: String, businessInformation: CompanyInformation): Future[Unit]
}

case class CompanyInformationMongo(name: String, consent: String, address: AddressInformation, lastUpdated: LocalDateTime) {
  def toCompanyInformation: CompanyInformation = CompanyInformation(name, consent, address)
}

object CompanyInformationMongo {
  implicit val mongoFormat: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[CompanyInformationMongo] = Json.format[CompanyInformationMongo]
}


