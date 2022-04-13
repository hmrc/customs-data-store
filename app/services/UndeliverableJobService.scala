/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{FailedToProcess, NoDataToProcess, ProcessResult, ProcessSucceeded, UndeliverableInformation}
import org.joda.time.DateTime
import play.api.Logging
import repositories.EmailRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UndeliverableJobService @Inject()(
                                         sub22Connector: Sub22Connector,
                                         emailRepository: EmailRepository
                                       )(implicit executionContext: ExecutionContext) extends Logging {
  def processJob(): Future[Seq[ProcessResult]] = {
    emailRepository.nextJobs.flatMap { notificationEmails =>
      Future.sequence(notificationEmails.map { notificationEmailMongo =>
        val notificationEmail = notificationEmailMongo.toNotificationEmail
        val maybeUndeliverableInformation: Option[UndeliverableInformation] = notificationEmail.undeliverable
        val maybeEori: Option[String] = maybeUndeliverableInformation.flatMap(_.extractEori)
        (maybeUndeliverableInformation, maybeEori) match {
          case (Some(undeliverableInformation), Some(eori)) =>
            updateSub22(undeliverableInformation, notificationEmail.timestamp, eori, notificationEmailMongo.undeliverable.map(_.attempts).getOrElse(1))
          case _ => Future.successful(NoDataToProcess)
        }
      })
    }
  }

  private def updateSub22(undeliverableInformation: UndeliverableInformation, timestamp: DateTime, eori: String, attempts: Int): Future[ProcessResult] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    sub22Connector.updateUndeliverable(undeliverableInformation, timestamp, attempts).flatMap { updateSuccessful =>
      if (updateSuccessful) {
        emailRepository.markAsSuccessful(eori).map { _ => ProcessSucceeded }
      } else {
        emailRepository.resetProcessing(eori).map { _ => FailedToProcess }
      }
    }
  }
}
