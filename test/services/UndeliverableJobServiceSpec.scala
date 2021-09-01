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

package services

import connectors.Sub22Connector
import models.{FailedToProcess, NoDataToProcess, NotificationEmail, ProcessSucceeded, UndeliverableInformation, UndeliverableInformationEvent}
import org.joda.time.DateTime
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
      when(mockEmailRepository.nextJob).thenReturn(Future.successful(None))
      running(app) {
        val result = await(service.processJob())
        result mustBe NoDataToProcess
      }
    }

    "return NoDataToProcess when no undeliverable information returned" in new Setup {
      when(mockEmailRepository.nextJob).thenReturn(Future.successful(Some(notificationEmail)))
      running(app) {
        val result = await(service.processJob())
        result mustBe NoDataToProcess
      }
    }

    "return NoDataToProcess when the eori cannot be extracted from the record" in new Setup {
      when(mockEmailRepository.nextJob).thenReturn(
        Future.successful(
          Some(notificationEmail.copy(undeliverable = Some(invalidEoriUndeliverableInformation)))
        )
      )

      running(app) {
        val result = await(service.processJob())
        result mustBe NoDataToProcess
      }
    }

    "return FailedToProcess when sub22Connector returns a failed response" in new Setup {
      when(mockEmailRepository.nextJob)
        .thenReturn(
          Future.successful(
            Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
          )
        )

      when(mockEmailRepository.resetProcessing(any()))
        .thenReturn(Future.successful(true))

      when(mockSub22Connector.updateUndeliverable(any(), any()))
        .thenReturn(
          Future.successful(false)
        )

      running(app) {
        val result = await(service.processJob())
        result mustBe FailedToProcess
      }
    }

    "return ProcessSucceeded when sub22Connector returns a successful response" in new Setup {
      when(mockEmailRepository.nextJob)
        .thenReturn(
          Future.successful(
            Some(notificationEmail.copy(undeliverable = Some(undeliverableInformation)))
          )
        )

      when(mockEmailRepository.markAsSuccessful(any()))
        .thenReturn(Future.successful(true))

      when(mockSub22Connector.updateUndeliverable(any(), any()))
        .thenReturn(
          Future.successful(true)
        )

      running(app) {
        val result = await(service.processJob())
        result mustBe ProcessSucceeded
      }
    }
  }


  trait Setup {
    val mockSub22Connector: Sub22Connector = mock[Sub22Connector]
    val mockEmailRepository: EmailRepository = mock[EmailRepository]


    val invalidEoriUndeliverableInformationEvent: UndeliverableInformationEvent =
      UndeliverableInformationEvent(
        "id",
        "someEvent",
        "some@email.com",
        DateTime.now().toString(),
        None,
        None,
        "invalid-eori"
      )

    val undeliverableInformationEvent: UndeliverableInformationEvent =
      UndeliverableInformationEvent(
        "id",
        "someEvent",
        "some@email.com",
        DateTime.now().toString(),
        None,
        None,
        "HMRC-CUS-ORG~EORINumber~GB123456789012"
      )

    val invalidEoriUndeliverableInformation: UndeliverableInformation = UndeliverableInformation("someSubject", "someEventId", "someGroupId", DateTime.now(), invalidEoriUndeliverableInformationEvent)
    val undeliverableInformation: UndeliverableInformation = UndeliverableInformation("someSubject", "someEventId", "someGroupId", DateTime.now(), undeliverableInformationEvent)

    val notificationEmail: NotificationEmail = NotificationEmail("some@email.com", DateTime.now(), None)

    val app: Application = application.overrides(
      inject.bind[Sub22Connector].toInstance(mockSub22Connector),
      inject.bind[EmailRepository].toInstance(mockEmailRepository)
    ).build()

    val service: UndeliverableJobService = app.injector.instanceOf[UndeliverableJobService]
  }
}
