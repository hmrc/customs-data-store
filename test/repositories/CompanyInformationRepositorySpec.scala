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

import models.{AddressInformation, CompanyInformation}
import play.api.Application
import play.api.test.Helpers.running
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CompanyInformationRepositorySpec extends SpecBase {


  "retrieve the company information from the database if present" in new Setup {
    running(app) {
      await(for {
        _ <- repository.set("testEori", companyInformation)
        result <- repository.get("testEori")
        _ <- dropData()
      } yield {
        result mustBe Some(companyInformation)
      })
    }
  }

  "return None if no company information present for the given EORI" in new Setup {
    running(app) {
      await(for {
        result <- repository.get("testEori")
        _ <- dropData()
      } yield {
        result mustBe None
      })
    }
  }

  trait Setup {
    val app: Application = application.build()
    val repository: DefaultCompanyInformationRepository = app.injector.instanceOf[DefaultCompanyInformationRepository]
    val addressInformation: AddressInformation = AddressInformation("12 Example Street", "Example", Some("AA00 0AA"), "GB")
    val companyInformation: CompanyInformation = CompanyInformation("Example Ltd", addressInformation)

    def dropData(): Future[Unit] = {
      repository.collection.drop().toFuture().map(_ => ())
    }
  }
}
