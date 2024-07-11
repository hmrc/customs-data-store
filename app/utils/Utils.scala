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

import uk.gov.hmrc.http.StringContextOps

import java.net.URL
import scala.util.Random

object Utils {

  val hyphen = "-"
  val emptyString = ""
  val singleSpace = " "
  val colon = ":"

  def randomUUID: String = java.util.UUID.randomUUID().toString

  private val acknowledgementRefLength = 32
  def acknowledgementReference: String = Random.alphanumeric.take(acknowledgementRefLength).mkString

  def uri(eori: String, endpoint: String): URL =
    url"$endpoint?regime=CDS&acknowledgementReference=$acknowledgementReference&EORI=$eori"
}
