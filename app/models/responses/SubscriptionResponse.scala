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

package models.responses

import models.{EORI, EmailAddress}
import play.api.libs.json.{Json, OFormat}

case class SubscriptionResponse(subscriptionDisplayResponse: SubscriptionDisplayResponse)

case class SubscriptionDisplayResponse(responseCommon: SubResponseCommon, responseDetail: Option[SubResponseDetail])

case class CdsEstablishmentAddress(
  streetAndNumber: String,
  city: String,
  postalCode: Option[String],
  countryCode: String
)

case class ContactInformation(
  personOfContact: Option[String],
  sepCorrAddrIndicator: Option[Boolean],
  streetAndNumber: Option[String],
  city: Option[String],
  postalCode: Option[String],
  countryCode: Option[String],
  telephoneNumber: Option[String],
  faxNumber: Option[String],
  emailAddress: Option[EmailAddress],
  emailVerificationTimestamp: Option[String]
)

case class VatId(countryCode: Option[String], VATID: Option[String])

case class SubResponseCommon(
  status: String,
  statusText: Option[String],
  processingDate: String,
  returnParameters: Option[Array[ReturnParameters]]
)

case class SubResponseDetail(
  EORINo: Option[EORI],
  EORIStartDate: Option[String],
  EORIEndDate: Option[String],
  CDSFullName: String,
  CDSEstablishmentAddress: CdsEstablishmentAddress,
  establishmentInTheCustomsTerritoryOfTheUnion: Option[String],
  typeOfLegalEntity: Option[String],
  contactInformation: Option[ContactInformation],
  VATIDs: Option[Array[VatId]],
  thirdCountryUniqueIdentificationNumber: Option[Array[String]],
  consentToDisclosureOfPersonalData: Option[String],
  shortName: Option[String],
  dateOfEstablishment: Option[String],
  typeOfPerson: Option[String],
  principalEconomicActivity: Option[String],
  ETMP_Master_Indicator: Boolean,
  XI_Subscription: Option[XiSubscription]
)

case class ReturnParameters(paramName: String, paramValue: String)

case class XiSubscription(
  XI_EORINo: String,
  PBEAddress: Option[PbeAddress],
  establishmentInTheCustomsTerritoryOfTheUnion: Option[String],
  XI_VATNumber: Option[String],
  EU_VATNumber: Option[Array[EUVATNumber]],
  XI_ConsentToDisclose: String,
  XI_SICCode: Option[String]
)

case class PbeAddress(
  pbeAddressLine1: String,
  pbeAddressLine2: Option[String],
  pbeAddressLine3: Option[String],
  pbeAddressLine4: Option[String],
  pbePostCode: Option[String]
)

case class EUVATNumber(countryCode: Option[String], VATId: Option[String])

object SubscriptionResponse {
  implicit val pbeAddressFormat: OFormat[PbeAddress]                                   = Json.format[PbeAddress]
  implicit val euVatFormat: OFormat[EUVATNumber]                                       = Json.format[EUVATNumber]
  implicit val xiSubscriptionFormat: OFormat[XiSubscription]                           = Json.format[XiSubscription]
  implicit val returnParametersFormat: OFormat[ReturnParameters]                       = Json.format[ReturnParameters]
  implicit val vatIDFormat: OFormat[VatId]                                             = Json.format[VatId]
  implicit val contactInformationFormat: OFormat[ContactInformation]                   = Json.format[ContactInformation]
  implicit val cdsEstablishmentAddressFormat: OFormat[CdsEstablishmentAddress]         = Json.format[CdsEstablishmentAddress]
  implicit val responseDetailFormat: OFormat[SubResponseDetail]                        = Json.format[SubResponseDetail]
  implicit val responseCommonFormat: OFormat[SubResponseCommon]                        = Json.format[SubResponseCommon]
  implicit val subscriptionDisplayResponseFormat: OFormat[SubscriptionDisplayResponse] =
    Json.format[SubscriptionDisplayResponse]

  implicit val responseSubscriptionFormat: OFormat[SubscriptionResponse] = Json.format[SubscriptionResponse]
}
