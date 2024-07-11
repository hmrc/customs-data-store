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

package utils

import utils.Utils.*

import java.net.URL

class UtilsSpec extends SpecBase {

  "hyphen" should {
    "return correct value" in {
      hyphen mustBe "-"
    }
  }

  "emptyString" should {
    "return correct value" in {
      emptyString mustBe empty
    }
  }

  "singleSpace" should {
    "return correct value" in {
      singleSpace mustBe " "
    }
  }

  "colon" should {
    "return correct value" in {
      colon mustBe ":"
    }
  }

  "acknowledgementReference" should {
    "return the correct length" in {
      acknowledgementReference.length mustBe 32
    }

    "return a value containing only Alphanumerics" in {
      acknowledgementReference.forall(_.isLetterOrDigit) mustBe true
    }
  }

  "randomUUID" should {
    "return a random number" in {
      val res = randomUUID

      res.isInstanceOf[String]
      res.length mustBe 36
    }
  }

  "uri" should {
    "return correct URL" in {
      val eori = "test_eori"
      val endPoint = "http://localhost:9893/test"
      val actualURL = uri(eori, endPoint)
      val expectedURLPart1 = s"$endPoint?regime=CDS&acknowledgementReference="
      val expectedURLPart2 = s"&EORI=$eori"

      actualURL.isInstanceOf[URL] mustBe true
      actualURL.toString.contains(expectedURLPart1) mustBe true
      actualURL.toString.contains(expectedURLPart2) mustBe true
    }
  }
}
