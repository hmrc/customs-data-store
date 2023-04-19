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

import models.{AddressInformation, CompanyInformation, XiEoriAddressInformation, XiEoriInformation}
import play.api.Application
import play.api.test.Helpers.running
import utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class XiEoriInformationRepositorySpec extends SpecBase {


  "retrieve the xi eori information from the database if present" in new Setup {
    running(app) {
      await(for {
        _ <- repository.set("testEori", xiEoriInformation)
        result <- repository.get("testEori")
        _ <- dropData()
      } yield {
        result mustBe Some(xiEoriInformation)
      })
    }
  }

  "return None if no xi eori information present for the given EORI" in new Setup {
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
    val repository: DefaultXiEoriInformationRepository = app.injector.instanceOf[DefaultXiEoriInformationRepository]
    val xiEoriAddressInformation: XiEoriAddressInformation = XiEoriAddressInformation("12 Example Street", None, "Example", "GB", Some("AA00 0AA"))
    val xiEoriInformation: XiEoriInformation = XiEoriInformation("XI123456789000", "1", xiEoriAddressInformation)

    def dropData(): Future[Unit] = {
      repository.collection.drop().toFuture().map(_ => ())
    }
  }
}
