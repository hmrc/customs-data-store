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

package services

import connectors.Sub22Connector
import models.repositories.{NotificationEmailMongo, UndeliverableInformationMongo}
import models.{FailedToProcess, NoDataToProcess, ProcessSucceeded, UndeliverableInformationEvent}
import java.time.LocalDateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.api.{Application, inject}
import repositories.EmailRepository
import utils.SpecBase

import scala.concurrent.Future

class UndeliverableJobServiceSpec extends SpecBase {

  "processJob" should {

    "return NoDataToProcess when no data returned from mongo" in new Setup {
      when(mockEmailRepository.nextJobs).thenReturn(Future.successful(Seq.empty))

      running(app) {
        val result = await(service.processJob())

        result mustBe Seq.empty
      }
    }

    "return NoDataToProcess when no undeliverable information returned" in new Setup {
      when(mockEmailRepository.nextJobs).thenReturn(Future.successful(Seq(notificationEmail)))

      running(app) {
        val result = await(service.processJob())

        result mustBe Seq(NoDataToProcess)
      }
    }

    "return NoDataToProcess when the eori cannot be extracted from the record" in new Setup {
      when(mockEmailRepository.nextJobs).thenReturn(
        Future.successful(
          Seq(notificationEmail.copy(undeliverable = Some(invalidEoriUndeliverableInformationMongo)))
        )
      )

      running(app) {
        val result = await(service.processJob())

        result mustBe Seq(NoDataToProcess)
      }
    }

    "return FailedToProcess when sub22Connector returns a failed response" in new Setup {
      when(mockEmailRepository.nextJobs)
        .thenReturn(
          Future.successful(
            Seq(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
          )
        )

      when(mockEmailRepository.resetProcessing(any()))
        .thenReturn(Future.successful(true))

      when(mockSub22Connector.updateUndeliverable(any(), any(), any())(any()))
        .thenReturn(
          Future.successful(false)
        )

      running(app) {
        val result = await(service.processJob())

        result mustBe Seq(FailedToProcess)
      }
    }

    "return ProcessSucceeded when sub22Connector returns a successful response" in new Setup {
      when(mockEmailRepository.nextJobs)
        .thenReturn(
          Future.successful(
            Seq(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
          )
        )

      when(mockEmailRepository.markAsSuccessful(any()))
        .thenReturn(Future.successful(true))

      when(mockSub22Connector.updateUndeliverable(any(), any(), any())(any()))
        .thenReturn(
          Future.successful(true)
        )

      running(app) {
        val result = await(service.processJob())

        result mustBe Seq(ProcessSucceeded)
      }
    }
  }

  trait Setup {
    val invalidEoriUndeliverableInformationEvent: UndeliverableInformationEvent =
      UndeliverableInformationEvent(
        "id",
        "someEvent",
        "some@email.com",
        LocalDateTime.now().toString(),
        None,
        None,
        "invalid-eori",
        Some("sdds")
      )

    val undeliverableInformationEvent: UndeliverableInformationEvent =
      UndeliverableInformationEvent(
        "id",
        "someEvent",
        "some@email.com",
        LocalDateTime.now().toString(),
        None,
        None,
        "HMRC-CUS-ORG~EORINumber~GB123456789012",
        Some("sdds")
      )

    val invalidEoriUndeliverableInformationMongo: UndeliverableInformationMongo =
      UndeliverableInformationMongo(
        "someSubject",
        "someEventId",
        "someGroupId",
        LocalDateTime.now(),
        invalidEoriUndeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )

    val undeliverableInformation: UndeliverableInformationMongo =
      UndeliverableInformationMongo(
        "someSubject",
        "someEventId",
        "someGroupId",
        LocalDateTime.now(),
        undeliverableInformationEvent,
        notifiedApi = false,
        processed = false
      )

    val notificationEmail: NotificationEmailMongo = NotificationEmailMongo("some@email.com", LocalDateTime.now(), None)

    val mockSub22Connector: Sub22Connector   = mock[Sub22Connector]
    val mockEmailRepository: EmailRepository = mock[EmailRepository]

    val app: Application = application
      .overrides(
        inject.bind[Sub22Connector].toInstance(mockSub22Connector),
        inject.bind[EmailRepository].toInstance(mockEmailRepository)
      )
      .build()

    val service: UndeliverableJobService = app.injector.instanceOf[UndeliverableJobService]
  }
}
