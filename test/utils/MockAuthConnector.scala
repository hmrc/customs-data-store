/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import actionbuilders.CustomAuthConnector
import config.Platform.{ENROLMENT_IDENTIFIER, ENROLMENT_KEY}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.TestData.TEST_EORI_VALUE

import scala.concurrent.Future

trait MockAuthConnector {
  val mockAuthConnector: CustomAuthConnector = mock[CustomAuthConnector]

  val enrolments: Enrolments = Enrolments(
    Set(Enrolment(ENROLMENT_KEY, Seq(EnrolmentIdentifier(ENROLMENT_IDENTIFIER, TEST_EORI_VALUE)), "activated"))
  )

  when(mockAuthConnector.authorise[Enrolments](any, any)(any, any)).thenReturn(Future.successful(enrolments))
}
