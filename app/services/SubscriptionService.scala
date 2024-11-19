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

import connectors.Sub09Connector
import models.{EORI, EmailAddress, NotificationEmail}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models.responses.{ContactInformation, EmailUnverifiedResponse, EmailVerifiedResponse}
import repositories.EmailRepository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Singleton
class SubscriptionService @Inject()(emailRepository: EmailRepository,
                                    sub09Connector: Sub09Connector)(implicit ec: ExecutionContext) {

  def getVerifiedEmail(eori: EORI): Future[EmailVerifiedResponse] = {

    emailRepository.get(eori.value).flatMap {
      case Some(notificationEmail) => Future.successful(
        EmailVerifiedResponse(Some(EmailAddress(notificationEmail.address))))

      case None => processEmailVerificationTimestampFromETMP(eori)
    }
  }

  private def processEmailVerificationTimestampFromETMP(eori: EORI): Future[EmailVerifiedResponse] = {

    getContactDetailsFromETMP(eori).flatMap {
      case Some(contactInfo) if contactInfo.emailVerificationTimestamp.isDefined =>
        updateNotificationEmailInRepo(eori.value, contactInfo)

      case _ => Future.successful(EmailVerifiedResponse(None))
    }
  }

  private def getContactDetailsFromETMP(eori: EORI): Future[Option[ContactInformation]] = {

    sub09Connector.retrieveSubscriptions(eori).map {
      _.flatMap(subsRes => subsRes.subscriptionDisplayResponse.responseDetail.contactInformation)
    }
  }

  private def updateNotificationEmailInRepo(eori: String,
                                            contactInfo: ContactInformation): Future[EmailVerifiedResponse] = {
    val localDateTime = LocalDateTime.parse(
      contactInfo.emailVerificationTimestamp.get, DateTimeFormatter.ISO_DATE_TIME)

    emailRepository.set(eori, NotificationEmail(contactInfo.emailAddress.get.value, localDateTime, None))
    Future.successful(EmailVerifiedResponse(contactInfo.emailAddress))
  }

  def getEmailAddress(eori: EORI): Future[EmailVerifiedResponse] = {
    for {
      optSubscription <- sub09Connector.retrieveSubscriptions(eori)
    } yield {
      optSubscription.fold(EmailVerifiedResponse(None)) {
        subsRes =>
          subsRes.subscriptionDisplayResponse.responseDetail.contactInformation match {
            case Some(ci) if ci.emailAddress.isDefined => EmailVerifiedResponse(ci.emailAddress)
            case _ => EmailVerifiedResponse(None)
          }
      }
    }
  }

  def getUnverifiedEmail(eori: EORI): Future[EmailUnverifiedResponse] = {
    for {
      optSubscription <- sub09Connector.retrieveSubscriptions(eori)
    } yield {
      optSubscription.fold(EmailUnverifiedResponse(None)) {
        subRes =>
          subRes.subscriptionDisplayResponse.responseDetail.contactInformation match {
            case Some(ci) if ci.emailVerificationTimestamp.isEmpty => EmailUnverifiedResponse(ci.emailAddress)
            case _ => EmailUnverifiedResponse(None)
          }
      }
    }
  }

}
