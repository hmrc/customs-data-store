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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(val configuration: Configuration, servicesConfig: ServicesConfig){
  //Remove duplicate / from urls read from config
  implicit class URLSyntacticSugar(left: String) {
    def /(right: String): String = removeTrailingSlash(left) + "/" + removeLeadingSlash(right)
    def removeTrailingSlash(in: String): String = if (in.last == '/') in.dropRight(1) else in
    def removeLeadingSlash(in: String): String = if (in.head == '/') in.drop(1) else in
  }

  lazy val sub21EORIHistoryEndpoint: String = servicesConfig.baseUrl("sub21") / configuration.getOptional[String]("microservice.services.sub21.historicEoriEndpoint").getOrElse("/")
  lazy val sub21HostHeader: Option[String] = configuration.getOptional[String]("microservice.services.sub21.host-header")
  lazy val sub21BearerToken: String = "Bearer " +configuration.getOptional[String]("microservice.services.sub21.bearer-token").getOrElse("secret-token")
  lazy val sub09GetSubscriptionsEndpoint: String = servicesConfig.baseUrl("sub09") / configuration.getOptional[String]("microservice.services.sub09.companyInformationEndpoint").getOrElse("/")
  lazy val sub09HostHeader: Option[String] = configuration.getOptional[String]("microservice.services.sub09.host-header")
  lazy val sub09BearerToken: String = "Bearer " +configuration.getOptional[String]("microservice.services.sub09.bearer-token").getOrElse("secret-token")
}


