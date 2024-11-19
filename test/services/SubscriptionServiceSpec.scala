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
import models.repositories.SuccessfulEmail
import models.{EORI, EmailAddress, NotificationEmail}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import play.api.{Application, inject}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase
import models.responses.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import repositories.EmailRepository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends SpecBase {

  "SubscriptionService" when {

    "get verifiedEmail" should {

      "return verified email with timestamp from ETMP if the data is unavailable in cache" in new Setup {

        when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))
        when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

        when(mockSub09Connector.retrieveSubscriptions(EORI("Trader EORI"))).thenReturn(
          Future.successful(Some(subscriptionResponseWithTimestamp)))

        running(app) {
          val result = service.getVerifiedEmail(EORI("Trader EORI"))
          result.map {
            ev => ev mustBe EmailVerifiedResponse(Option(EmailAddress(emailAddress)))
          }

          verify(mockEmailRepository, times(1)).set(any(), any())
          verify(mockEmailRepository, times(1)).get(any())
          verify(mockSub09Connector, times(1)).retrieveSubscriptions(any())
        }
      }

      "return verified email if the data is available in cache" in new Setup {

        when(mockEmailRepository.get(any())).thenReturn(Future.successful(Some(notificationEmail)))

        running(app) {
          val result = service.getVerifiedEmail(EORI("Trader EORI"))
          result.map {
            ev => ev mustBe EmailVerifiedResponse(Option(EmailAddress(emailAddress)))
          }

          verify(mockEmailRepository, times(0)).set(any(), any())
          verify(mockEmailRepository, times(1)).get(any())
          verify(mockSub09Connector, times(0)).retrieveSubscriptions(any())
        }
      }

      "return None if the data is unavailable in cache and there is no contactInformation from ETMP" in new Setup {

        when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))
        when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

        when(mockSub09Connector.retrieveSubscriptions(EORI("Trader EORI"))).thenReturn(
          Future.successful(Some(subscriptionResponse)))

        running(app) {
          val result = service.getVerifiedEmail(EORI("Trader EORI"))
          result.map {
            ev => ev mustBe EmailVerifiedResponse(None)
          }

          verify(mockEmailRepository, times(0)).set(any(), any())
          verify(mockEmailRepository, times(1)).get(any())
          verify(mockSub09Connector, times(1)).retrieveSubscriptions(any())
        }
      }

      "return None if there is no timestamp in contactInformation" in new Setup {

        when(mockEmailRepository.get(any())).thenReturn(Future.successful(None))
        when(mockEmailRepository.set(any(), any())).thenReturn(Future.successful(SuccessfulEmail))

        when(mockSub09Connector.retrieveSubscriptions(EORI("Trader EORI"))).thenReturn(
          Future.successful(Some(subscriptionResponseWithContactInfo)))

        running(app) {
          val result = service.getVerifiedEmail(EORI("Trader EORI"))
          result.map {
            ev => ev mustBe EmailVerifiedResponse(None)
          }

          verify(mockEmailRepository, times(0)).set(any(), any())
          verify(mockEmailRepository, times(1)).get(any())
          verify(mockSub09Connector, times(1)).retrieveSubscriptions(any())
        }
      }
    }

    "getEmailAddress" should {
      "return correct output when there is no contactInformation" in new Setup {
        when(mockSub09Connector.retrieveSubscriptions(any))
          .thenReturn(Future.successful(Some(subscriptionResponse)))

        running(app) {
          val result: Future[EmailVerifiedResponse] = service.getEmailAddress(EORI("Trader EORI"))

          result.map {
            ev => ev mustBe EmailVerifiedResponse(None)
          }
        }
      }

      "return correct output when contactInformation is available" in new Setup {
        when(mockSub09Connector.retrieveSubscriptions(any))
          .thenReturn(Future.successful(Some(subscriptionResponseWithContactInfo)))

        running(app) {
          val result: Future[EmailVerifiedResponse] = service.getEmailAddress(EORI("Trader EORI"))

          result.map {
            ev => ev mustBe EmailVerifiedResponse(Option(EmailAddress(emailAddress)))
          }
        }
      }
    }

    "getUnverifiedEmail" should {
      "return correct output when there is no contactInformation" in new Setup {
        when(mockSub09Connector.retrieveSubscriptions(any))
          .thenReturn(Future.successful(Some(subscriptionResponse)))

        running(app) {
          val result: Future[EmailUnverifiedResponse] = service.getUnverifiedEmail(EORI("Trader EORI"))

          result.map {
            eunv => eunv mustBe EmailUnverifiedResponse(None)
          }
        }
      }

      "return correct output when contactInformation is available" in new Setup {
        when(mockSub09Connector.retrieveSubscriptions(any))
          .thenReturn(Future.successful(Some(subscriptionResponseWithContactInfo)))

        running(app) {
          val result: Future[EmailUnverifiedResponse] = service.getUnverifiedEmail(EORI("Trader EORI"))

          result.map {
            eunv => eunv mustBe EmailUnverifiedResponse(Option(EmailAddress(emailAddress)))
          }
        }
      }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val eori: EORI = EORI("testEORI")
    val mockSub09Connector: Sub09Connector = mock[Sub09Connector]
    val mockEmailRepository: EmailRepository = mock[EmailRepository]

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[Sub09Connector].toInstance(mockSub09Connector),
      inject.bind[EmailRepository].toInstance(mockEmailRepository)
    ).configure(
      "microservice.metrics.enabled" -> false,
      "metrics.enabled" -> false,
      "auditing.enabled" -> false
    ).build()


    val dateTimeString: String = "2020-10-05T09:30:47Z"
    val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)

    val service: SubscriptionService = app.injector.instanceOf[SubscriptionService]

    val responseCommon: SubResponseCommon = SubResponseCommon("OK", None, dateTimeString, None)
    val cdsEstablishmentAddress: CdsEstablishmentAddress = CdsEstablishmentAddress(
      "1 street", "Southampton", Some("SO1 1AA"), "GB")

    val vatIds: VatId = VatId(Some("abc"), Some("123"))
    val euVatIds: EUVATNumber = EUVATNumber(Some("def"), Some("456"))

    val xiEoriAddress: PbeAddress = PbeAddress("1 Test street", Some("city A"), Some("county"), None, Some("AA1 1AA"))
    val xiEoriSubscription: XiSubscription = XiSubscription("XI1234567", Some(xiEoriAddress), Some("1"),
      Some("12345"), Some(Array(euVatIds)), "1", Some("abc"))

    val responseDetail: SubResponseDetail = SubResponseDetail(Some(EORI("someEori")), None, None, "CDSFullName",
      cdsEstablishmentAddress, Some("0"), None, None, Some(Array(vatIds)),
      None, None, None, None, None, None, ETMP_Master_Indicator = true, Some(xiEoriSubscription))

    val emailAddress: String = "test@gmil.com"

    val contactInfo: ContactInformation = ContactInformation(None, None, None, None, None, None, None, None,
      emailAddress = Some(EmailAddress(emailAddress)), None)

    val contactInfoWithTimeStamp: ContactInformation = ContactInformation(None, None, None, None, None, None, None, None,
      emailAddress = Some(EmailAddress(emailAddress)), Some(dateTimeString))

    val responseDetailWithContactInfo: SubResponseDetail = responseDetail.copy(contactInformation = Some(contactInfo))

    val responseDetailWithTimestamp: SubResponseDetail =
      responseDetail.copy(contactInformation = Some(contactInfoWithTimeStamp))

    val subscriptionResponse: SubscriptionResponse = SubscriptionResponse(
      SubscriptionDisplayResponse(responseCommon, responseDetail))

    val subscriptionResponseWithContactInfo: SubscriptionResponse = SubscriptionResponse(
      SubscriptionDisplayResponse(responseCommon, responseDetailWithContactInfo))

    val subscriptionResponseWithTimestamp: SubscriptionResponse = SubscriptionResponse(
      SubscriptionDisplayResponse(responseCommon, responseDetailWithTimestamp))

    val notificationEmail: NotificationEmail = NotificationEmail(emailAddress, dateTime, None)
  }
}
