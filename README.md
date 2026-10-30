# customs-data-store

## About

This microservice provides a persistent cache that holds customs-related data, including EORI history, company information, verified email addresses and undeliverable email information.

This microservice is a common backend service, which is used by other services. For example, it is used by CDS Exports, CDS Financials and CDS Reimbursements.

| Key | Value |
|---|---|
| Digital service | CDS Financials |
| Local port | `9893` |
| Base path | `/customs-data-store` |
| Front-end | Not applicable — this is a shared backend service |
| Acceptance tests | [customs-financials-acceptance-tests](https://github.com/hmrc/customs-financials-acceptance-tests) |
| Performance tests | [customs-financials-performance-tests](https://github.com/hmrc/customs-financials-performance-tests) |
| Stubs used locally | `CUSTOMS_FINANCIALS_HODS_STUB` |

## How to Run This Service

### Prerequisites

Use [Service Manager](#service-manager-profiles) to start the services that this microservice depends on:

```bash
sm2 --start CUSTOMS_FINANCIALS_ALL
```

### Running the Service Locally

To run this service from source (for example, to test your own changes), stop the instance started by Service Manager and run it with sbt:

```bash
sm2 --stop CUSTOMS_DATA_STORE
sbt run
```

The service runs on port `9893` by default. You can also specify the port explicitly:

```bash
sbt "run 9893"
```

## How to Test This Service

### Local

#### Unit Tests

Run the unit tests locally:

```bash
sbt test
```

To run tests for a single file:

```bash
sbt "test/testOnly *TEST_FILE_NAME*"
```

#### Pre-Push Check

The `runAllChecks` sbt alias runs the standard checks configured for this project, including compilation, coverage, unit tests, integration tests, formatting checks, Scala style checks and the coverage report.

It is good practice to run it just before pushing to GitHub:

```bash
sbt runAllChecks
```


#### Acceptance Tests (smoke and regression)
The acceptance tests for this service are in [customs-financials-acceptance-tests](https://github.com/hmrc/customs-financials-acceptance-tests). For information about how to run them, and which environments they run against, see [README](https://github.com/hmrc/customs-financials-acceptance-tests#how-to-run-the-tests).

#### Performance tests
The performance tests for this service are in [customs-financials-performance-tests](https://github.com/hmrc/customs-financials-performance-tests). For information about how to run them, and which environments they run against, see [README](https://github.com/hmrc/customs-financials-performance-tests#how-to-run-the-tests).

### Manual Testing in QA and Staging
After the successful [deployment pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSFinancials/job/customs-data-store-pipeline/), manual testing is performed in the QA and/or Staging environments, see [Manual Service Verification in QA and Staging](https://github.com/hmrc/customs-financials-acceptance-tests#manual-testing-in-qa-and-staging).

## Service Catalogue

- [customs-data-store in the MDTP Catalogue](https://catalogue.tax.service.gov.uk/repositories/customs-data-store)

## Jenkins Pipeline

- [Build pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSFinancials/job/customs-data-store/)
- [Deployment pipeline](https://build.tax.service.gov.uk/job/BordersAndTradeLiveServices/job/CDSFinancials/job/customs-data-store-pipeline/)

- The acceptance test Jenkins jobs (smoke and regression) are listed in the [customs-financials-acceptance-tests README](https://github.com/hmrc/customs-financials-acceptance-tests#how-to-run-the-tests).
- The performance test Jenkins jobs are listed in the [customs-financials-performance-tests README](https://github.com/hmrc/customs-financials-performance-tests#how-to-run-the-tests).

## Service Manager Profiles

These profiles are defined in [service-manager-config](https://github.com/hmrc/service-manager-config).

| Profile | Use | Services started |
|---|---|---|
| `CUSTOMS_FINANCIALS_ALL` | Running and developing this service locally | `CUSTOMS_FINANCIALS_FRONTEND`,`CUSTOMS_FINANCIALS_DOCUMENTS_FRONTEND`,`CUSTOMS_FINANCIALS_SECURE_MESSAGING_FRONTEND`,`ENTITY_RESOLVER`, `CHANNEL_PREFERENCES`, `SECURE_MESSAGE_FRONTEND`, `SECURE_MESSAGE`, `SECURE_MESSAGE_STUB`, `MESSAGE`, `CUSTOMS_DUTY_DEFERMENT_FRONTEND`, `CUSTOMS_MANAGE_AUTHORITIES_FRONTEND`, `CUSTOMS_CASH_ACCOUNT_FRONTEND`, `CUSTOMS_GUARANTEE_ACCOUNT_FRONTEND`, `CUSTOMS_HISTORIC_STATEMENT_FRONTEND`, `CUSTOMS_FINANCIALS_API`, `CUSTOMS_FINANCIALS_EMAIL_THROTTLER`, `AUTH`, `AUTH_LOGIN_STUB`, `AUTH_LOGIN_API`, `CENTRALISED_AUTHORISATION_SERVER`, `SSO`, `USER_DETAILS`, `FEEDBACK_FRONTEND`, `CUSTOMS_FINANCIALS_HODS_STUB`, `CUSTOMS_FINANCIALS_SDES_STUB`, `CUSTOMS_FINANCIALS_SDDS_STUB`, `IDENTITY_VERIFICATION`, `BAS_STUBS`, `BAS_GATEWAY`, `BAS_PROXY`, `BAS_GATEWAY_FRONTEND`, `BAS_STUB_FRONTEND`, `TWO_STEP_VERIFICATION_RULE_ENGINE`, `TAR_FRONTEND`, `DATASTREAM`, `EMAIL`, `HMRC_EMAIL_RENDERER`, `CUSTOMS_DATA_STORE`, `CUSTOMS_FINANCIALS_SESSION_CACHE`, `TRACKING_CONSENT_FRONTEND`, `CONTACT_FRONTEND`, `ACCESSIBILITY_STATEMENT_FRONTEND`  |

| Service | Port | Repository |
|---|---:|---|
| `CUSTOMS_DATA_STORE` | 9893 | This service |
| `CUSTOMS_FINANCIALS_HODS_STUB` | 9753 | [customs-financials-hods-stub](https://github.com/hmrc/customs-financials-hods-stub) |

## Endpoints

All paths below are relative to the service root (`http://localhost:9893/customs-data-store` when running locally).

The authenticated endpoints retrieve the EORI from the user's authentication enrolment. The third-party and update endpoints accept service-to-service requests and do not use the same authenticated-request action.

| Method | Path | Purpose | Sample request | Response |
|---|---|---|---|---|
| GET | `/eori/verified-email` | Retrieves a verified email address for the logged-in EORI, from cache or SUB09 | No request body | `200` with email details; `404` if no email is found; `500` on an unexpected failure |
| GET | `/eori/company-information` | Retrieves the business name and address for the logged-in EORI, from cache or SUB09 | No request body | `200` with company information; `404` if information is not found |
| GET | `/eori/eori-history` | Retrieves the GB EORI history associated with the logged-in EORI | No request body | `200` with EORI history |
| GET | `/eori/gbxi-eori-history` | Retrieves the GB and XI EORI history associated with the logged-in EORI | No request body | `200` with EORI history |
| GET | `/eori/xieori-information` | Retrieves XI EORI information for the logged-in EORI | No request body | `200` with XI EORI information; `404` if information is not found |
| POST | `/eori/verified-email-third-party` | Retrieves a verified email address for the EORI specified in the request body | `{"eori":"GB123456789000"}` | `200` with email details; `400` for a malformed request; `404` if no email is found; `500` on an unexpected failure |
| POST | `/eori/company-information-third-party` | Retrieves company information for the EORI specified in the request body | `{"eori":"GB123456789000"}` | `200` with company information; `400` for a malformed request; `404` if information is not found |
| POST | `/eori/xieori-information-third-party` | Retrieves XI EORI information for the EORI specified in the request body | `{"eori":"GB123456789000"}` | `200` with XI EORI information; `400` for a malformed request; `404` if information is not found |
| POST | `/eori/eori-history-third-party` | Retrieves GB EORI history for the EORI specified in the request body | `{"eori":"GB123456789000"}` | `200` with EORI history |
| POST | `/eori/gbxi-eori-history-third-party` | Retrieves GB and XI EORI history for the EORI specified in the request body | `{"eori":"GB123456789000"}` | `200` with EORI history |
| POST | `/update-email` | Populates a verified email address in the cache and removes undeliverable information | See [Update Email](#update-email) | `204` on success; `500` if the cache update fails |
| POST | `/update-eori-history` | Updates the EORI history in the cache for a given EORI | See [Update EORI History](#update-eori-history) | `204` on success; `404` if no history is found; `500` if the update fails |
| POST | `/update-undeliverable-email` | Updates undeliverable information for an EORI enrolment | See [Update Undeliverable Email](#update-undeliverable-email) | `204` when processed; `400` for an invalid request; `404` if no update is performed; `500` on an unexpected failure |
| GET | `/subscriptions/subscriptionsdisplay` | Retrieves verified email information for the logged-in EORI | No request body | `200` with email response JSON; `503` if the downstream request fails |
| GET | `/subscriptions/unverified-email-display` | Retrieves unverified email information for the logged-in EORI | No request body | `200` with email response JSON; `503` if the downstream request fails |
| GET | `/subscriptions/email-display` | Retrieves email information for the logged-in EORI | No request body | `200` with email response JSON; `503` if the downstream request fails |

### Enrolment Required

The following endpoints use the authenticated user's EORI:

- `GET /eori/verified-email`
- `GET /eori/company-information`
- `GET /eori/eori-history`
- `GET /eori/gbxi-eori-history`
- `GET /eori/xieori-information`
- `GET /subscriptions/subscriptionsdisplay`
- `GET /subscriptions/unverified-email-display`
- `GET /subscriptions/email-display`

| Field | Value |
|---|---|
| Local Auth Login Stub URL | `http://localhost:9949/auth-login-stub/gg-sign-in` |
| Redirect URL | Not applicable to this backend service; use the consuming frontend's redirect URL when signing in through a frontend |
| Enrolment Key | `HMRC-CUS-ORG` |
| Identifier Name | `EORINumber` |
| Identifier Value | Use an EORI configured for the local test environment. Example values used by CDS Financials development services include `GB744638982000` and `GB744638982001`. |

### Example payloads

#### GET /eori/verified-email

An endpoint to retrieve a verified email address for the logged-in EORI.

Example response:

```json
{
  "address": "test@email.com",
  "timestamp": "2020-03-20T01:02:03Z"
}
```

Response codes:

| Status | Description |
|---|---|
| `200` | A verified email has been found for the specified EORI |
| `404` | No verified email has been found for the specified EORI |
| `500` | An unexpected failure happened in the service |

#### GET /eori/company-information

An endpoint to retrieve the business name and address for the logged-in EORI.

Example response:

```json
{
  "name": "ABC Ltd",
  "consent": "1",
  "address": {
    "streetAndNumber": "12 Example Street",
    "city": "Example",
    "postalCode": "AA00 0AA",
    "countryCode": "GB"
  }
}
```

Response codes:

| Status | Description |
|---|---|
| `200` | Company information found and returned |
| `404` | Company information or required payload elements not found |

#### GET /eori/eori-history and GET /eori/gbxi-eori-history

These endpoints retrieve historic EORI numbers associated with an EORI. The first endpoint returns GB history; the second includes GB and XI history.

Example response:

```json
{
  "eoriHistory": [
    {
      "eori": "GB123456789000",
      "validFrom": "2001-01-20",
      "validUntil": "2001-01-20"
    },
    {
      "eori": "XI123456789000",
      "validFrom": "2001-01-20",
      "validUntil": "2001-01-20"
    }
  ]
}
```

#### GET /eori/xieori-information

An endpoint that retrieves XI EORI information for the requested EORI.

Example response:

```json
{
  "xiEori": "XI744638982004",
  "consent": "S",
  "address": {
    "pbeAddressLine1": "address line 1",
    "pbeAddressLine2": "address line 2",
    "pbeAddressLine3": "city 1",
    "pbePostCode": "AA1 1AA"
  }
}
```

Response codes:

| Status | Description |
|---|---|
| `200` | XI EORI information is returned |
| `404` | XI EORI information is not found |

#### POST /eori/verified-email-third-party, /eori/company-information-third-party, /eori/xieori-information-third-party and EORI history third-party endpoints

These endpoints accept an EORI in the request body to retrieve information for a specified EORI, rather than the logged-in user's EORI.

Example request:

```json
{
  "eori": "GB123456789000"
}
```

The response depends on the endpoint called. See the relevant GET endpoint examples above for the email, company information, XI EORI information and EORI history response structures.

#### Update Email

`POST /update-email`

An endpoint to update the verified email address for a given EORI and remove undeliverable information.

Example request:

```json
{
  "eori": "GB123456789000",
  "address": "test@email.com",
  "timestamp": "2020-03-20T01:02:03"
}
```

Response codes:

| Status | Description |
|---|---|
| `204` | Email successfully updated |
| `500` | The cache update failed |

#### Update EORI History

`POST /update-eori-history`

An endpoint to update the historic EORI information in the cache for a given EORI.

Example request:

```json
{
  "eori": "GB123456789000",
  "validFrom": "2001-01-20",
  "validUntil": "2001-01-20"
}
```

Response codes:

| Status | Description |
|---|---|
| `204` | Successfully updated the historic EORI information in the cache |
| `404` | No historic EORI information was found to update |
| `500` | An unexpected failure happened in the service |

#### Update Undeliverable Email

`POST /update-undeliverable-email`

An endpoint to update undeliverable information for an EORI enrolment.

Example request:

```json
{
  "subject": "subject-example",
  "eventId": "example-id",
  "groupId": "example-group-id",
  "timestamp": "2021-05-14T10:59:45.811+01:00",
  "event": {
    "id": "example-id",
    "event": "someEvent",
    "emailAddress": "email@email.com",
    "detected": "2021-05-14T10:59:45.811+01:00",
    "code": 12,
    "reason": "Inbox full",
    "enrolment": "HMRC-CUS-ORG~EORINumber~GB123456789000"
  }
}
```
Response codes:

| Status | Description |
|---|---|
| `204` | Undeliverable information successfully processed |
| `404` | No update was performed, for example because the record does not exist or already contains the same information |
| `400` | Invalid request or enrolment key/identifier |
| `500` | An unexpected failure happened in the service |

#### Subscription email endpoints

The following endpoints return email information for the logged-in EORI:

- `GET /subscriptions/subscriptionsdisplay`
- `GET /subscriptions/unverified-email-display`
- `GET /subscriptions/email-display`

Example verified email response:

```json
{
  "verifiedEmail": "test@test.com"
}
```

Example unverified email response:

```json
{
  "unVerifiedEmail": "test@test.com"
}
```

## Developer Notes

### Feature Flags

This service has a feature flag named `sub24-enabled`, which defaults to `false` in `application.conf`. It controls the configured SUB24/SUB21 integration behaviour for EORI history requests.

Do not enable feature flags in `application.conf`, as this can apply globally by default. To enable the flag locally without changing the configuration file, run:

```bash
sbt "run -Dfeatures.sub24-enabled=true"
```

### Scalafmt

The code is formatted with [sbt-scalafmt](https://scalameta.org/scalafmt/docs/installation.html#sbt), using the rules in `.scalafmt.conf`.

Check that all project files are formatted as expected:

```bash
sbt scalafmtCheckAll scalafmtSbtCheck
```

Format `*.sbt` and `project/*.scala` files:

```bash
sbt scalafmtSbt
```

Format all project files:

```bash
sbt scalafmtAll
```
## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
