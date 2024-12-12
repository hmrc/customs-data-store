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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.Utils.singleSpace

@Singleton
class AppConfig @Inject() (val configuration: Configuration, servicesConfig: ServicesConfig) {
  private val bearerTokenPrefix = "Bearer"

  lazy val schedulerDelay: Int       = configuration.get[Int]("scheduler.initialDelaySeconds")
  lazy val schedulerMaxAttempts: Int = configuration.get[Int]("scheduler.maxAttempts")

  lazy val authUrl: String = servicesConfig.baseUrl("auth")

  lazy val sub09GetSubscriptionsEndpoint: String =
    servicesConfig.baseUrl("sub09") / configuration.get[String](
      "microservice.services.sub09.companyInformationEndpoint"
    )

  lazy val sub09BearerToken: String =
    s"$bearerTokenPrefix$singleSpace${configuration.get[String]("microservice.services.sub09.bearer-token")}"

  lazy val sub21EORIHistoryEndpoint: String =
    servicesConfig.baseUrl("sub21") / configuration.get[String]("microservice.services.sub21.historicEoriEndpoint")

  lazy val sub21BearerToken: String =
    s"$bearerTokenPrefix$singleSpace${configuration.get[String]("microservice.services.sub21.bearer-token")}"

  lazy val sub22UpdateVerifiedEmailEndpoint: String =
    servicesConfig.baseUrl("sub22") / configuration.get[String](
      "microservice.services.sub22.updateVerifiedEmailEndpoint"
    )

  lazy val sub22BearerToken: String =
    s"$bearerTokenPrefix$singleSpace${configuration.get[String]("microservice.services.sub22.bearer-token")}"

  implicit class URLSyntacticSugar(left: String) {
    def /(right: String): String = removeTrailingSlash(left) + "/" + removeLeadingSlash(right)

    private def removeTrailingSlash(in: String): String = if (in.last == '/') in.dropRight(1) else in

    private def removeLeadingSlash(in: String): String = if (in.head == '/') in.drop(1) else in
  }
}
