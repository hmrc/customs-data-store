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

import models.requests.{RequestCommon, RequestDetail, Sub22Request, Sub22UpdateVerifiedEmailRequest}
import models.{UndeliverableInformation, UndeliverableInformationEvent}
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers._
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.SpecBase

import scala.concurrent._

class AuditingServiceSpec extends SpecBase {

  "AuditingService" should {
    "audit the bounced email request data" in new Setup {
      val extendedDataEventCaptor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val undeliverableInformation: UndeliverableInformation = UndeliverableInformation(
        "bounced-email",
        "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
        "20180622211249.1.2A6098970A380E12@example.org",
        DateTime.parse("2021-04-07T09:46:29+00:00"),
        UndeliverableInformationEvent(
          "L4XgfOuWSpCJVjF8T9ipRw",
          "failed",
          "hmrc-customer@some-domain.org",
          "2021-04-07T09:46:29+00:00",
          Some(605),
          Some("Not delivering to previously bounced address"),
          "HMRC-CUS-ORG~EORINumber~GB744638982000"
        )
      )

      val request =
        """{
          |    "subject": "bounced-email",
          |    "eventId" : "77ed39b7-d5d8-46ed-abab-a5a8ff416dae",
          |    "groupId": "20180622211249.1.2A6098970A380E12@example.org",
          |    "timestamp" : "2021-04-07T09:46:29.000Z",
          |    "event" : {
          |        "id": "L4XgfOuWSpCJVjF8T9ipRw",
          |        "event": "failed",
          |        "emailAddress": "hmrc-customer@some-domain.org",
          |        "detected": "2021-04-07T09:46:29+00:00",
          |        "code": "605",
          |        "reason": "Not delivering to previously bounced address",
          |        "enrolment": "HMRC-CUS-ORG~EORINumber~GB744638982000"
          |  }
          |}""".stripMargin


      running(app) {
        when(mockAuditConnector.sendExtendedEvent(extendedDataEventCaptor.capture())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        service.auditBouncedEmail(undeliverableInformation)
        val result = extendedDataEventCaptor.getValue
        result.detail mustBe Json.parse(request)
        result.auditType mustBe "BouncedEmail"
        result.auditSource mustBe "customs-data-store"
        result.tags.get("transactionName") mustBe Some("Bounced Email")

      }
    }

    "audit the SUB22 request data" in new Setup {
      val extendedDataEventCaptor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      val time = DateTime.parse("2021-10-06T12:32:28Z")

      val sub22Request = Sub22UpdateVerifiedEmailRequest(
        Sub22Request(
          RequestCommon("CDS", time, "8e61730857ae46a28d9c76ec39a52099"),
          RequestDetail("EORI", "GB333186848876", "test@email.com", time, emailVerified = false)
        )
      )

      val request =
        s"""{
          |    "updateVerifiedEmailRequest":{
          |      "attempts":1,
          |      "successful": false,
          |      "requestCommon":{
          |        "regime":"CDS",
          |        "receiptDate":"2021-10-06T12:32:28Z",
          |        "acknowledgementReference":"8e61730857ae46a28d9c76ec39a52099"
          |        },
          |      "requestDetail":{
          |        "IDType":"EORI",
          |        "IDNumber":"GB333186848876",
          |        "emailAddress":"test@email.com",
          |        "emailVerificationTimestamp":"2021-10-06T12:32:28Z",
          |        "emailVerified":false
          |        }
          |      }
          |  }""".stripMargin


      running(app) {
        when(mockAuditConnector.sendExtendedEvent(extendedDataEventCaptor.capture())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        service.auditSub22Request(sub22Request, 1, successful = false)
        val result = extendedDataEventCaptor.getValue
        result.detail mustBe Json.parse(request)
        result.auditType mustBe "UpdateVerificationTimestamp"
        result.auditSource mustBe "customs-data-store"
        result.tags.get("transactionName") mustBe Some("Update Verification Timestamp")

      }
    }
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val mockAuditConnector: AuditConnector = mock[AuditConnector]

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[AuditConnector].toInstance(mockAuditConnector)
    ).build()

    val service: AuditingService = app.injector.instanceOf[AuditingService]
  }
}
