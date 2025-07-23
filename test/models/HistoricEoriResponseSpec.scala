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

import models.responses.{GetEORIHistoryResponse, ResponseCommon, ResponseDetail}
import utils.SpecBase
import utils.TestData.{DATE_STRING, TEST_EORI_VALUE}
import play.api.libs.json.{JsResultException, JsSuccess, Json}

class HistoricEoriResponseSpec extends SpecBase {

  "HistoricEoriResponse" should {
    "generate correct output for Json Reads" in new Setup {
      import HistoricEoriResponse.reads

      Json.fromJson(Json.parse(historicEoriResponseObJsString)) mustBe JsSuccess(historicEoriResponseOb)
    }

    "generate correct output for Json Writes" in new Setup {
      import HistoricEoriResponse.historicEoriResponseFormat
      Json.toJson(historicEoriResponseOb) mustBe Json.parse(historicEoriResponseObJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"historyResponse\": \"pending\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[HistoricEoriResponse]
      }
    }
  }

  trait Setup {
    val resCommon: ResponseCommon = ResponseCommon("OK", DATE_STRING)
    val eoriHistory: EORIHistory  = EORIHistory(TEST_EORI_VALUE, Some(DATE_STRING), Some(DATE_STRING))

    val resDetail: ResponseDetail = ResponseDetail(Seq(eoriHistory))

    val getEpriHistoryResponseOb: GetEORIHistoryResponse = GetEORIHistoryResponse(resCommon, resDetail)

    val historicEoriResponseOb: HistoricEoriResponse = HistoricEoriResponse(getEpriHistoryResponseOb)

    val historicEoriResponseObJsString: String =
      """{"getEORIHistoryResponse":{
        |"responseCommon":{"status":"OK","processingDate":"2024-07-22"},
        |"responseDetail":
        |{"EORIHistory":[{"EORI":"test_eori","validFrom":"2024-07-22","validTo":"2024-07-22"}]}}
        |}""".stripMargin
  }
}
