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

import play.api.libs.json.Format
import play.api.mvc.PathBindable
import utils.JsonFormatUtils

final case class EORI(value: String)

object EORI {
  implicit val format: Format[EORI] = JsonFormatUtils.stringFormat(EORI.apply)(_.value)

  implicit def pathBinder(implicit stringBinder: PathBindable[String]): PathBindable[EORI] = new PathBindable[EORI] {
    override def bind(key: String, value: String): Either[String, EORI] =
      stringBinder.bind(key, value).map(EORI(_))

    override def unbind(key: String, eori: EORI): String = eori.value
  }
}
