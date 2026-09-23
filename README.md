# customs-data-store

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Coverage](https://img.shields.io/badge/test_coverage-90-green.svg)](/target/scala-3.7.0/scoverage-report/index.html) [![Accessibility](https://img.shields.io/badge/WCAG2.2-AA-purple.svg)](https://www.gov.uk/service-manual/helping-people-to-use-your-service/understanding-wcag)

This repository contains the code for a persistent cache that holds customs-related data.

This microservice is a common backend service, which is used by other services. For example, it is used by CDS Exports, CDS Financials and CDS Reimbursements.

##  Pre-requisites

There are a number of dependencies required to run the service.

The easiest way to get started with these is via the Service Manager command line interface. You can find the installation
instructions in the [MDTP HandBook](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html).

| Command                              | Description                                                      |
|--------------------------------------|------------------------------------------------------------------|
| `sm2 --start CUSTOMS_FINANCIALS_ALL` | Runs all dependencies                                            |
| `sm2 -s`                             | Shows running services                                           |
| `sm2 --stop CUSTOMS_DATA_STORE`      | Stop the micro service                                           |
| `sbt run` or `sbt "run 9893"`        | (from root dir) to compile the current service with your changes |

## Testing

The minimum requirement for test coverage is 90%. Builds will fail when the project drops below this threshold.

### Unit tests

| Command                                | Description                  |
|----------------------------------------|------------------------------|
| `sbt test`                             | Runs unit tests locally      |
| `sbt "test/testOnly *TEST_FILE_NAME*"` | runs tests for a single file |

### Coverage

| Command                                  | Description                                                                                                 |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `sbt clean coverage test coverageReport` | Generates a unit test coverage report that you can find here target/scala-3.3.5/scoverage-report/index.html |

## Available routes

| Path                                                            | Description                                                                                                                        | Comments                                                                      |
|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| GET /customs-data-store/eori/verified-email                    | Retrieves a verified email address for the logged-in EORI, either from cache or SUB09                                              | Requires an EORI enrolment for 'HMRC-CUS-ORG'                                 |
| GET /customs-data-store/eori/company-information               | Retrieves the business full name and address for the logged-in EORI, either from cache or SUB09                                    | Requires an EORI enrolment for 'HMRC-CUS-ORG'                                 |
| GET /customs-data-store/eori/eori-history                      | Retrieves a list of all historic GB EORI's associated with the logged-in EORI, either from cache or SUB24                          | Requires an EORI enrolment for 'HMRC-CUS-ORG'                                 |
| GET /customs-data-store/eori/gbxi-eori-history                  | Retrieves a list of all historic GB and XI EORI's associated with the logged-in EORI, either from cache or SUB24                   | Requires an EORI enrolment for 'HMRC-CUS-ORG'; includes XI EORIs if available |
| GET /customs-data-store/eori/xieori-information                | Retrieves XI EORI information for the logged-in EORI, either from cache or SUB09                                                   | Requires an EORI enrolment for 'HMRC-CUS-ORG'                                 |
| POST /customs-data-store/eori/verified-email-third-party        | Retrieves the verified email address for the EORI specified in request body, either from cache or SUB09                            | The EORI provided in the request body must have an 'HMRC-CUS-ORG' enrolment.              |
| POST /customs-data-store/eori/company-information-third-party   | Retrieves the business full name for the EORI specified in request body, either from cache or SUB09                                | The EORI provided in the request body must have an 'HMRC-CUS-ORG' enrolment.                 |
| POST /customs-data-store/eori/xieori-information-third-party    | Retrieves XI EORI information for the EORI specified in request body, either from cache or SUB09                                   |                                                         |
| POST /customs-data-store/eori/eori-history-third-party           | Retrieves the historic GB EORIs for the EORI specified in request body from cache or SUB24                                         |                                      |
| POST /customs-data-store/eori/gbxi-eori-history-third-party      | Retrieves the historic GB and XI EORIs for the EORI specified in request body from cache or SUB24                                  |                                      |
| POST /customs-data-store/update-email                          | Populates a new verified email address in the cache and removes undeliverable information (cache write only, no upstream SUB call) |                                                                               |
| POST /customs-data-store/update-eori-history                   | Updates the eori history for a given EORI in the cache from the upstream service (calls SUB24)                                     |                                                                               |
| POST /customs-data-store/update-undeliverable-email             | Updates undeliverable information for a given enrolmentValue (calls SUB22 to propagate the bounce upstream)                        |                                                                               |
| GET /customs-data-store/subscriptions/subscriptionsdisplay     | Internal Use Only                                                                                                                  |                                                                               |
| GET /customs-data-store/subscriptions/unverified-email-display | Internal Use Only                                                                                                                  |                                                                               |
| GET /customs-data-store/subscriptions/email-display             | Internal Use Only                                                                                                                  |                                                                               |

## GET /eori/verified-email

An endpoint to retrieve a verified email address for the logged-in EORI (taken from the caller's EORI enrolment). If the email is not already cached it is retrieved from SUB09 and cached
before being returned.

The user/trader must be subscribed to CDS and have an `HMRC-CUS-ORG` enrolment on their EORI.

### Response body

```json
{
  "address": "test@email.com",
  "timestamp": "2020-03-20T01:02:03Z"
}
```

If this email address has previously been reported as undeliverable, an additional `undeliverable` object is
also present in the response.

### Response codes

| Status | Description                                                                                |
|--------|--------------------------------------------------------------------------------------------|
| 200    | A verified email has been found for the specified eori                                     |
| 403    | The user/trader does not have a valid EORI enrolment                                            |
| 404    | No verified email has been found for the specified eori, either in the cache or from SUB09 |
| 500    | An unexpected failure happened in the service (e.g. the cache could not be read/written)   |

**Note:** if the upstream SUB09 call itself fails (times out, errors, or returns an unparsable response), this is
treated as a 404 error, **not** a 500 error.

## GET /eori/company-information

An endpoint to retrieve the business full name and address for the logged-in EORI (taken from the caller's EORI
enrolment). If not already cached, this is retrieved from SUB09 and cached before being returned.

The user/trader must be subscribed to CDS and have an `HMRC-CUS-ORG` enrolment on their EORI.

### Response body

```json
{
  "name": "ABC ltd",
  "consent": "1",
  "address": {
    "streetAndNumber": "12 Example Street",
    "city": "Example",
    "postalCode": "AA00 0AA",
    "countryCode": "GB"
  }
}
```

### Fields

| Field                   | Required  | Description                                                                |
|-------------------------|-----------|-----------------------------------------------------------------------------|
| name                    | Mandatory | Company name                                                               |
| consent                 | Mandatory | consentToDisclosureOfPersonalData — defaults to "0" if not supplied upstream |
| address                 | Mandatory | The address Information for the company                                  |
| address.streetAndNumber | Mandatory | The street and number where the company resides                          |
| address.city            | Mandatory | The city where the company resides                                       |
| address.postalCode      | Optional  | Mandatory for the country code "GB"                               |
| address.countryCode     | Mandatory | The country code where the company resides                               |

### Response codes

| Status | Description                                                                                |
|--------|----------------------------------------------------------------------------------------------|
| 200    | Company information found and returned                                                      |
| 403    | The user/trader does not have a valid EORI enrolment                                             |
| 404    | Company information not found in the cache or from SUB09 (this also covers a failed SUB09 call) |
| 500    | An unexpected failure happened in the service                                               |

## GET /eori/eori-history and GET /eori/gbxi-eori-history

Two endpoints retrieve a list of all historic EORI's associated with the logged-in EORI (taken from the caller's
EORI enrolment), either from cache or from the upstream EORI history service:

- `GET /eori/eori-history` returns **GB EORIs only**. Cached responses have any `XI`-prefixed EORIs explicitly
  filtered out; a fresh fetch from the upstream service relies on SUB24 not returning XI-associated EORIs
  when queried without `association=1`.
- `GET /eori/gbxi-eori-history` returns the full history, **including** any `XI`-prefixed EORIs.

Both require the user/trader to be authenticated with an EORI enrolment (`HMRC-CUS-ORG` / `EORINumber`).

### Response body

```json
{
  "eoriHistory": [
    {
      "eori": "GB987654321000",
      "validFrom": "2019-07-24",
      "validUntil": "2021-03-15"
    },
    {
      "eori": "GB111222333000",
      "validFrom": "2009-05-16"
    }
  ]
}
```

### Response codes

| Status | Description                                                                          |
|--------|----------------------------------------------------------------------------------------|
| 200    | The eori history has been returned. If none is found, an empty `eoriHistory` array is returned with a 200 |
| 403    | The user/trader does not have a valid EORI enrolment                                       |
| 500    | An unexpected failure happened in the service                                         |

## GET /eori/xieori-information

An endpoint that retrieves XI EORI information for the logged-in EORI (taken from the caller's EORI enrolment).

The user/trader must be subscribed to CDS and have an `HMRC-CUS-ORG` enrolment on their EORI.


### Response body

```json
{
  "xiEori": "XI123456789000",
  "consent": "S",
  "address": {
    "pbeAddressLine1": "address line 1",
    "pbeAddressLine2": "address line 2",
    "pbeAddressLine3": "city 1",
    "pbeAddressLine4": "county 1",
    "pbePostCode": "AA1 1AA"
  }
}
```

### Fields

| Field                   | Required  | Description                                             |
|-------------------------|-----------|-----------------------------------------------------------|
| xiEori                  | Mandatory | The XI EORI number |
| consent                 | Mandatory | XI_ConsentToDisclose for the XI subscription               |
| address                 | Mandatory | The XI PBE address information                            |
| address.pbeAddressLine1 | Mandatory | First line of the address  |
| address.pbeAddressLine2 | Optional  |                                                             |
| address.pbeAddressLine3 | Optional  |                                                             |
| address.pbeAddressLine4 | Optional  |                                                             |
| address.pbePostCode     | Optional  |                                                             |

### Response codes

| Status | Description                                                                        |
|--------|--------------------------------------------------------------------------------------|
| 200    | XI EORI information is returned  |
| 403    | The user/trader does not have a valid EORI enrolment                                     |
| 404    | XI EORI information is retrieved neither from cache nor SUB09                       |
| 500    | An unexpected failure happened in the service                                       |

## POST /eori/verified-email-third-party

An endpoint to retrieve a verified email address for the EORI specified in the request body. Unlike
`GET /eori/verified-email`, this endpoint does **not** require the caller to hold an EORI enrolment — the EORI
whose data is returned is whatever is supplied in the request body.

### Example request

```json
{
  "eori": "GB123456789000"
}
```

### Fields

| Field | Required  | Description                                          |
|-------|-----------|------------------------------------------------------|
| eori  | Mandatory | The eori used to provide a verified email address to |

### Response body

```json
{
  "address": "test@email.com",
  "timestamp": "2020-03-20T01:02:03Z"
}
```

If this email address has previously been reported as undeliverable, an additional `undeliverable` object is
also present in the response.

### Response codes

| Status | Description                                                                                   |
|--------|-----------------------------------------------------------------------------------------------|
| 200    | A verified email has been found for the specified eori                                        |
| 400    | Malformed request (the request body could not be parsed)                                      |
| 404    | No verified email has been found for the specified eori, either in the cache or from SUB09 |
| 500    | An unexpected failure happened in the service                                                 |

## POST /eori/company-information-third-party

An endpoint to retrieve the business full name and address for the EORI specified in the request body. This
endpoint does **not** require the caller to hold an EORI enrolment.

### Example request

```json
{
  "eori": "GB123456789000"
}
```

### Fields

| Field | Required  | Description                                             |
|-------|-----------|----------------------------------------------------------|
| eori  | Mandatory | The eori to retrieve business/company information for   |

### Response body

```json
{
  "name": "ABC ltd",
  "consent": "1",
  "address": {
    "streetAndNumber": "12 Example Street",
    "city": "Example",
    "postalCode": "AA00 0AA",
    "countryCode": "GB"
  }
}
```

### Fields

| Field                   | Required  | Description                                                                |
|-------------------------|-----------|-----------------------------------------------------------------------------|
| name                    | Mandatory | Company name                                                               |
| consent                 | Mandatory | consentToDisclosureOfPersonalData — defaults to "0" if not supplied upstream |
| address                 | Mandatory | The address Information for the company                                  |
| address.streetAndNumber | Mandatory | The street and number where the company resides                          |
| address.city            | Mandatory | The city where the company resides                                       |
| address.postalCode      | Optional  | Mandatory for the country code "GB"             |
| address.countryCode     | Mandatory | The country code where the company resides                               |

### Response codes

| Status | Description                                                                                |
|--------|-----------------------------------------------------------------------------------------------|
| 200    | Company information found and returned                                                       |
| 400    | Malformed request                                     |
| 404    | Company information not found in the cache or from SUB09 (this also covers a failed SUB09 call) |
| 500    | An unexpected failure happened in the service                                                |

## POST /eori/xieori-information-third-party

An endpoint that retrieves XI EORI information for the EORI specified in the request body. This endpoint does
**not** require the caller to hold an EORI enrolment.


### Example request

```json
{
  "eori": "GB123456789000"
}
```

### Fields

| Field | Required  | Description                                |
|-------|-----------|---------------------------------------------|
| eori  | Mandatory | The eori to retrieve XI EORI information for |

### Response body

```json
{
  "xiEori": "XI123456789000",
  "consent": "S",
  "address": {
    "pbeAddressLine1": "address line 1",
    "pbeAddressLine2": "address line 2",
    "pbeAddressLine3": "city 1",
    "pbeAddressLine4": "county 1",
    "pbePostCode": "AA1 1AA"
  }
}
```

### Response codes

| Status | Description                                                                        |
|--------|--------------------------------------------------------------------------------------|
| 200    | XI EORI information is returned  |
| 400    | Malformed request                             |
| 404    | XI EORI information is retrieved neither from cache nor SUB09                       |
| 500    | An unexpected failure happened in the service                                       |

## POST /eori/eori-history-third-party and POST /eori/gbxi-eori-history-third-party

Two endpoints retrieve the historic EORIs of a given third party EORI (not the caller's own EORI, and no EORI
enrolment is required):

- `POST /eori/eori-history-third-party` returns **GB EORIs only**. Cached responses have any `XI`-prefixed
  EORIs explicitly filtered out; a fresh fetch from the upstream service relies on SUB24 not returning
  XI-associated EORIs when queried without `association=1`.
- `POST /eori/gbxi-eori-history-third-party` returns the full history, **including** any `XI`-prefixed EORIs.

### Example request

```json
{
  "eori": "GB123456789000"
}
```

### Fields

| Field | Required  | Description                                                          |
|-------|-----------|------------------------------------------------------------------------|
| eori  | Mandatory | The eori for which historically associated EORIs are to be retrieved |

### Response body

```json
{
  "eoriHistory": [
    {
      "eori": "GB987654321000",
      "validFrom": "2019-07-24",
      "validUntil": "2021-03-15"
    },
    {
      "eori": "GB111222333000",
      "validFrom": "2009-05-16"
    }
  ]
}
```

### Response codes

| Status | Description                                                                         |
|--------|-----------------------------------------------------------------------------------------|
| 200    | The eori history has been returned. |
| 400    | Malformed request                             |
| 500    | An unexpected failure happened in the service                                         |

## POST /update-email

An endpoint to update the verified email address for a given EORI and remove any undeliverable information
previously held for it. This is a cache write only — it does not call any upstream SUB service.

### Example request

```json
{
  "eori": "GB123456789000",
  "address": "test@email.com",
  "timestamp": "2020-03-20T01:02:03Z"
}
```

### Fields

| Field     | Required  | Description                                          |
|-----------|-----------|------------------------------------------------------|
| eori      | Mandatory | The eori used to provide a verified email address to |
| address   | Mandatory | The verified email address for the specified eori    |
| timestamp | Mandatory | The timestamp when the email was verified            |

### Response codes

| Status | Description                                                |
|--------|--------------------------------------------------------------|
| 204    | The email address was updated in the cache                  |
| 400    | Malformed request    |
| 500    | An unexpected failure happened in the service                |

## POST /update-eori-history

An endpoint that refreshes the historic EORI's for a given EORI in the cache. The `eori` in the request body is
used to look up the EORI's full history from the upstream service (SUB24) — that fresh upstream history is
what gets cached.

### Example request

```json
{
  "eori": "GB123456789000"
}
```

### Response codes

| Status | Description                                                                                  |
|--------|--------------------------------------------------------------------------------------------------|
| 204    | Successfully refreshed the historic EORI's in the cache from the upstream service               |
| 400    | Malformed request (the request body could not be parsed)                                        |
| 404    | The upstream service returned no history at all for this eori (the cache is left unchanged)     |
| 500    | An unexpected failure happened in the service                                                    |

## POST /update-undeliverable-email

An endpoint to update undeliverable information for an enrolmentValue. Once the cache is updated, it also calls
SUB22 to propagate the undeliverable/bounce information upstream.

### Request parameters

| Param              | Type     | Optional/Mandatory |
|--------------------|----------|--------------------|
| subject            | String   | Mandatory          |
| eventId            | String   | Mandatory          |
| groupId            | String   | Mandatory          |
| timestamp          | DateTime | Mandatory          |
| event.id           | String   | Mandatory          |
| event.enrolment    | String   | Mandatory          |
| event.emailAddress | String   | Mandatory          |
| event.event        | String   | Mandatory          |
| event.detected     | DateTime | Mandatory          |
| event.code         | Int      | Optional           |
| event.reason       | String   | Optional           |
| event.source       | String   | Optional           |

### Example request

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

### Response codes

| Status | Description                                                                                                                    |
|--------|--------------------------------------------------------------------------------------------------------------------------------|
| 204    | Successfully updated undeliverable information for enrolmentValue                                                              |
| 404    | No update was performed on a record either due to it not existing or already having the same undeliverable information present |
| 400    | If enrolmentKey is not equal to 'HMRC-CUS-ORG' OR If enrolmentIdentifier is not equal to 'EORINumber'                          |
| 500    | An unexpected failure happened in the service                                                                                  |

## Helpful commands

| Command                                       | Description                                                                                                 |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `sbt runAllChecks`                            | Runs all standard code checks                                                                               |
| `sbt clean`                                   | Cleans code                                                                                                 |
| `sbt compile`                                 | Better to say 'Compiles the code'                                                                           |
| `sbt coverage`                                | Prints code coverage                                                                                        |
| `sbt test`                                    | Runs unit tests                                                                                             |
| `sbt it/test`                                 | Runs integration tests                                                                                      |
| `sbt scalafmtCheckAll`                        | Runs code formatting checks based on .scalafmt.conf                                                         |
| `sbt scalastyle`                              | Runs code style checks based on /scalastyle-config.xml                                                      |
| `sbt Test/scalastyle`                         | Runs code style checks for unit test code /test-scalastyle-config.xml                                       |
| `sbt coverageReport`                          | Produces a code coverage report                                                                             |
| `sbt "test/testOnly *TEST_FILE_NAME*"`        | runs tests for a single file                                                                                |
| `sbt clean coverage test coverageReport`      | Generates a unit test coverage report that you can find here target/scala-3.3.5/scoverage-report/index.html |
| `sbt "run -Dfeatures.some-feature-name=true"` | enables a feature locally without risking exposure                                                          |

