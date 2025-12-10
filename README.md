
# customs-data-store

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Coverage](https://img.shields.io/badge/test_coverage-90-green.svg)](/target/scala-3.7.0/scoverage-report/index.html) [![Accessibility](https://img.shields.io/badge/WCAG2.2-AA-purple.svg)](https://www.gov.uk/service-manual/helping-people-to-use-your-service/understanding-wcag)

This repository contains the code for a persistent cache holding customs related data.

This Microservice is a common backend service and used by other CDS teams (Exports and Reimbursement) as well.

## Running the service

*From the root directory*

`sbt run` - starts the service locally

`sbt runAllChecks` - Will run all checks required for a successful build

Default service port on local - 9893

### Required dependencies

There are a number of dependencies required to run the service.

The easiest way to get started with these is via the service manager CLI - you can find the installation guide [here](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html)

| Command                              | Description                                                      |
|--------------------------------------|------------------------------------------------------------------|
| `sm2 --start CUSTOMS_FINANCIALS_ALL` | Runs all dependencies                                            |
| `sm2 -s`                             | Shows running services                                           |
| `sm2 --stop CUSTOMS_DATA_STORE`      | Stop the micro service                                           |
| `sbt run` or `sbt "run 9893"`        | (from root dir) to compile the current service with your changes |

## Running and testing on localhost:
If you want to run [customs-data-store](https://github.com/hmrc/customs-data-store) locally then you also have to run [customs-financials-hods-stub](https://github.com/hmrc/customs-financials-hods-stub) so that it can retrieve historic Eoris from there.  
To start the service from sbt: `sbt "run 9893" ` or from service manager: `sm --start CUSTOMS_DATA_STORE CUSTOMS_FINANCIALS_HODS_STUB -f`  
In Postman
1. Send in any of the below requests to http://localhost:9893/customs-data-store/
2. Add and `Authorization` header and set its value to whatever is in `application.conf  ` under the key `server-token`

### Runtime Dependencies
(These are subject to change and may not include every dependency)

* `AUTH`
* `AUTH_LOGIN_STUB`
* `AUTH_LOGIN_API`
* `BAS_GATEWAY`
* `CA_FRONTEND`
* `SSO`
* `USER_DETAILS`
* `CUSTOMS_FINANCIALS_SDES_STUB`

### Login enrolments

The service's endpoints (that need Enrolment to access) can be accessed by using below enrolments.

| Enrolment Key	 | Identifier Name | Identifier Value |
|----------------|-----------------|------------------|
| `HMRC-CUS-ORG` | `EORINumber`    | `GB744638982000` |
| `HMRC-CUS-ORG` | `EORINumber`    | `GB744638982001` |

## Testing

The minimum requirement for test coverage is 90%. Builds will fail when the project drops below this threshold.

### Unit Tests

| Command                                | Description                  |
|----------------------------------------|------------------------------|
| `sbt test`                             | Runs unit tests locally      |
| `sbt "test/testOnly *TEST_FILE_NAME*"` | runs tests for a single file |

### Coverage

| Command                                  | Description                                                                                                 |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `sbt clean coverage test coverageReport` | Generates a unit test coverage report that you can find here target/scala-3.3.5/scoverage-report/index.html |

## Available Routes

| Path                                                          | Description                                                                                            | Comments                                                                                                                                                                  |
|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GET /customs-data-store/eori/:eori/verified-email             | Retrieve the verified email address for a given EORI either from the cache or SUB09                    | <span style="color: red">Decommissioning soon, use either /customs-data-store/eori/verified-email or /customs-data-store/eori/verified-email-third-party</span>           |
| GET /customs-data-store/eori/xieori-information               | Retrieves the XI EORI information for the requested EORI either from the cache or SUB09                |                                                                                                                                                                           |
| POST /customs-data-store/eori/verified-email-third-party      | Retrieves the verified email address for the EORI specified in request body either from cache or SUB09 |                                                                                                                                                                           |
| POST /customs-data-store/eori/company-information-third-party | Retrieves the business full name for the EORI specified in request body                                |                                                                                                                                                                           |
| POST /customs-data-store/eori/eori-history-third-party        | Retrieves the historic EORIs for a given third-party EORI                                              |                                                                                                                                                                           |
| POST /customs-data-store/update-email                         | Populates a new verified email address in the cache and removes undeliverable information              |                                                                                                                                                                           |
| POST /customs-data-store/update-eori-history                  | Updates the eori history for a given EORI in the cache                                                 |                                                                                                                                                                           |
| POST /update-undeliverable-email                              | Updates undeliverable information for a given enrolmentValue                                           |                                                                                                                                                                           |

## Feature Switches
Not applicable

## GET /eori/:eori/verified-email (<span style="color: red">Decommissioning soon</span>)

An endpoint to retrieve a verified email address for a given EORI.

### Response body

```json
{
  "address" : "test@email.com",
  "timestamp" : "2020-03-20T01:02:03Z"
}
```

### Response codes

| Status | Description                                             |
|--------|---------------------------------------------------------|
| 200    | A verified email has been found for the specified eori  |
| 404    | No verified email has been found for the specified eori |
| 500    | An unexpected failure happened in the service           |

## GET /eori/verified-email

An endpoint to retrieve a verified email address for logged-in EORI.

### Response body

```json
{
  "address" : "test@email.com",
  "timestamp" : "2020-03-20T01:02:03Z"
}
```

### Response codes

| Status | Description                                             |
|--------|---------------------------------------------------------|
| 200    | A verified email has been found for the specified eori  |
| 404    | No verified email has been found for the specified eori |
| 500    | An unexpected failure happened in the service           |

## GET /eori/company-information

An endpoint to retrieve the business full name and address for logged-in EORI.

## Response body

```json
{
  "name": "ABC ltd",
  "consent": "1",
  "address" : {
    "streetAndNumber": "12 Example Street",
    "city": "Example",
    "postalCode": "AA00 0AA",
    "countryCode": "GB"
  }
}
```

### Fields

| Field                               | Required                                          | Description                                          |
| ---------------------------------  | ---------------------------------------------------- | ---------------------------------------------------- |
| name | Mandatory        | Company name        |
| consent | Optional        | consentToDisclosureOfPersonalData        |
| address | Mandatory        | The address Information for the company        |
| address.streetAndNumber | Mandatory | The street and number where the company resides |
| address.city | Mandatory | The city where the company resides |
| address.postalCode | Optional | Mandatory for the country code "GB" |
| address.countryCode | Mandatory | The country code where the company resides |

### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 200 | Company information found and returned        |
| 404 | Company information not found or elements of the payload not found |

## POST /update-email

An endpoint to update the verified email address for a given EORI and removes undeliverable information.

### Example request

```json
{
  "eori" : "someEori",
  "address" : "test@email.com",
  "timestamp" : "2020-03-20T01:02:03Z"
}
```

### Fields

| Field                               | Required                                          | Description                                          |
| ---------------------------------  | ---------------------------------------------------- | ---------------------------------------------------- |
| eori | Mandatory        | The eori used to provide a verified email address to        |
| address | Mandatory        | The verified email address for the specified eori        |
| timestamp | Mandatory | The timestamp when the email was verified |


## GET /eori/eori-history

An endpoint that provides a list of all historic EORI's associated for logged-in EORI.

### Response body

```json
{
"eoriHistory": [
  {
    "eori": "historicEori1", 
    "validFrom": "2001-01-20T00:00:00Z", 
    "validTo": "2001-01-20T00:00:00Z"
  },
  {
    "eori": "historicEori2",
    "validFrom": "2001-01-20T00:00:00Z",
    "validTo": "2001-01-20T00:00:00Z"
  }
]
}
```
### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 200 | A sequence of historic eori's returned        |
| 500 | An unexpected failure happened in the service |


## GET /eori/xieori-information

An endpoint that provides XI EORI information for the requested EORI.

### Response body

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
### Response codes

| Status | Description                                                   |
|--------|---------------------------------------------------------------|
| 200    | XI EORI information is returned                               |
| 404    | XI EORI information is retrieved neither from cache nor SUB09 |

## POST /eori/verified-email-third-party

An endpoint to retrieve a verified email address for EORI specified in request body.

### Example request

```json
{
  "eori" : "testEori"
}
```

### Fields

| Field                               | Required                                          | Description                                          |
| ---------------------------------  | ---------------------------------------------------- | ---------------------------------------------------- |
| eori | Mandatory        | The eori used to provide a verified email address to        |

### Response body

```json
{
  "address" : "test@email.com",
  "timestamp" : "2020-03-20T01:02:03Z"
}
```

### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 200 | A verified email has been found for the specified eori        |
| 400 | Malformed request |
| 404 | No verified email has been found for the specified eori        |
| 500 | An unexpected failure happened in the service |

## POST /eori/company-information-third-party

An endpoint to retrieve the business full name and address for EORI specified in request body.

### Example request

```json
{
  "eori" : "testEori"
}
```
### Fields

| Field                               | Required                                          | Description                                          |
| ---------------------------------  | ---------------------------------------------------- | ---------------------------------------------------- |
| eori | Mandatory        | The eori used to provide a verified email address to        |

### Response body

```json
{
  "name": "ABC ltd",
  "consent": "1",
  "address" : {
    "streetAndNumber": "12 Example Street",
    "city": "Example",
    "postalCode": "AA00 0AA",
    "countryCode": "GB"
  }
}
```

### Fields

| Field                               | Required                                          | Description                                          |
| ---------------------------------  | ---------------------------------------------------- | ---------------------------------------------------- |
| name | Mandatory        | Company name        |
| consent | Optional        | consentToDisclosureOfPersonalData        |
| address | Mandatory        | The address Information for the company        |
| address.streetAndNumber | Mandatory | The street and number where the company resides |
| address.city | Mandatory | The city where the company resides |
| address.postalCode | Optional | Mandatory for the country code "GB" |
| address.countryCode | Mandatory | The country code where the company resides |

### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 200 | A verified email has been found for the specified eori        |
| 400 | Malformed request |
| 404 | No verified email has been found for the specified eori        |
| 500 | An unexpected failure happened in the service |


## POST /eori/eori-history-third-party

An endpoint to retrieve the historic EORIs of a given third party EORI (not the logged-in user's EORI).

### Example request

```json
{
  "eori" : "testEori"
}
```
### Fields

| Field                               | Required                                          | Description                                                          |
| ---------------------------------  | ---------------------------------------------------- |----------------------------------------------------------------------|
| eori | Mandatory        | The eori for which historically associated EORIs are to be retrieved |

### Response body

```json
{
"eoriHistory": [
  {
    "eori": "historicEori1", 
    "validFrom": "2001-01-20T00:00:00Z", 
    "validTo": "2001-01-20T00:00:00Z"
  },
  {
    "eori": "historicEori2",
    "validFrom": "2001-01-20T00:00:00Z",
    "validTo": "2001-01-20T00:00:00Z"
  }
]
}
```
### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 200 | A sequence of historic eori's returned        |
| 500 | An unexpected failure happened in the service |


## POST /update-eori-history

An endpoint to populate the historic EORI's for a given EORI.

### Example request

```json
{
  "eori" : "testEori"
}
```

### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 204 | Successfully updated the historic EORI's in the cache       |
| 500 | An unexpected failure happened in the service |

## POST /update-undeliverable-email

An endpoint to update undeliverable information for an enrolmentValue.

### Request parameters
| Param                               | Type                                          | Optional/Mandatory|
| ---------------------------------  | ---------------------------------------------------- | --- |
| subject | String       | Mandatory |
| eventId | String       | Mandatory |
| groupId | String       | Mandatory |
| timestamp | DateTime       | Mandatory |
| event.id | String       | Mandatory |
| event.enrolment | String       | Mandatory |
| event.emailAddress | String       | Mandatory |
| event.event | String       | Mandatory |
| event.detected | DateTime       | Mandatory |
| event.code | Int       | Optional |
| event.reason | String       | Optional |


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
    "enrolment": "HMRC-CUS-ORG~EORINumber~testEori"
  }
}
```

### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 204 | Successfully updated undeliverable information for enrolmentValue  |
| 404 | No update was performed on a record either due to it not existing or already having the same undeliverable information present |
| 400 | If enrolmentKey is not equal to 'HMRC-CUS-ORG' OR If enrolmentIdentifier is not equal to 'EORINumber' |
| 500 | An unexpected failure happened in the service |


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

