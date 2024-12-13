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

package models

import play.api.libs.json.Json
import utils.SpecBase

import java.time.LocalDateTime

class NotificationEmailSpec extends SpecBase {

  "Reads" should {

    "generate correct output" in new Setup {

      import NotificationEmail.emailFormat

      Json.fromJson(Json.parse(notificationEmailJsString)).isSuccess mustBe true
      Json.fromJson(Json.parse(notificationEmailJsString)).get.timestamp mustBe timeStamp
    }
  }

  "Writes" should {

    "generate correct output" in new Setup {
      Json.toJson(notifMailOb) mustBe Json.parse(notificationEmailJsStringForWrites)
    }

    "generate correct output when time has no seconds" in new Setup {
      Json.toJson(notifMailObWithNoSeconds) mustBe Json.parse(notificationEmailWith00SecondsJsStringForWrites)
    }
  }

  trait Setup {
    val year          = 2024
    val month         = 5
    val dayOfTheMonth = 17
    val hourOfTheDay  = 12
    val minutes       = 55
    val seconds       = 44

    val timeStamp: LocalDateTime              = LocalDateTime.of(year, month, dayOfTheMonth, hourOfTheDay, minutes, seconds)
    val timeStampWith00Seconds: LocalDateTime = LocalDateTime.of(year, month, dayOfTheMonth, hourOfTheDay, minutes)

    val undeliverableEvent: UndeliverableInformationEvent =
      UndeliverableInformationEvent(
        id = "test_id",
        event = "circuit_breaker",
        emailAddress = "test@abc.com",
        detected = "test",
        code = None,
        reason = None,
        enrolment = "test_enrol",
        source = None
      )

    val notifMailOb: NotificationEmail = NotificationEmail(
      address = "test_address",
      timeStamp,
      undeliverable =
        Some(UndeliverableInformation("test_sub", "test_event_id", "test_group_id", timeStamp, undeliverableEvent))
    )

    val notifMailObWithNoSeconds: NotificationEmail = NotificationEmail(
      address = "test_address",
      timeStampWith00Seconds,
      undeliverable =
        Some(UndeliverableInformation("test_sub", "test_event_id", "test_group_id", timeStamp, undeliverableEvent))
    )

    val notificationEmailJsString: String =
      """
        |{"address":"test_address",
        |"timestamp":"2024-05-17T12:55:44",
        |"undeliverable":{
        |"subject":"test_sub",
        |"eventId":"test_event_id",
        |"groupId":"test_group_id",
        |"timestamp":"2024-05-17T12:55:44",
        |"event":{
        |"id":"test_id",
        |"event":"circuit_breaker",
        |"emailAddress":"test@abc.com",
        |"detected":"test",
        |"enrolment":"test_enrol"
        |}}}""".stripMargin

    val notificationEmailJsStringForWrites: String =
      """
        |{"address":"test_address",
        |"timestamp":"2024-05-17T12:55:44Z",
        |"undeliverable":{
        |"subject":"test_sub",
        |"eventId":"test_event_id",
        |"groupId":"test_group_id",
        |"timestamp":"2024-05-17T12:55:44",
        |"event":{
        |"id":"test_id",
        |"event":"circuit_breaker",
        |"emailAddress":"test@abc.com",
        |"detected":"test",
        |"enrolment":"test_enrol"
        |}}}""".stripMargin

    val notificationEmailWith00SecondsJsStringForWrites: String =
      """
        |{"address":"test_address",
        |"timestamp":"2024-05-17T12:55:00Z",
        |"undeliverable":{
        |"subject":"test_sub",
        |"eventId":"test_event_id",
        |"groupId":"test_group_id",
        |"timestamp":"2024-05-17T12:55:44",
        |"event":{
        |"id":"test_id",
        |"event":"circuit_breaker",
        |"emailAddress":"test@abc.com",
        |"detected":"test",
        |"enrolment":"test_enrol"
        |}}}""".stripMargin
  }
}
