
# customs-data-store

This repository contains the code for a persistent cache holding customs related data.

| Path                               | Description                                                                               |
| ---------------------------------  |-------------------------------------------------------------------------------------------|
| GET /customs-data-store/eori/:eori/verified-email | Retrieve the verified email address for a given EORI either from the cache or SUB09       |
| GET /customs-data-store/eori/:eori/company-information | Retrieves the business full name and address for the given EORI                           |
| GET /customs-data-store/eori/:eori/eori-history | Retrieves the historic eori's for a given EORI either from the cache or SUB21             |
| GET /customs-data-store/eori/xieori-information | Retrieves the XI EORI information for the requested EORI either from the cache or SUB09   |
| POST /customs-data-store/update-email | Populates a new verified email address in the cache and removes undeliverable information | 
| POST /customs-data-store/update-eori-history | Updates the eori history for a given EORI in the cache                                    |
| POST /update-undeliverable-email | Updates undeliverable information for a given enrolmentValue                              |

## GET /eori/:eori/verified-email

An endpoint to retrieve a verified email address for a given EORI

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
| 404 | No verified email has been found for the specified eori        |
| 500 | An unexpected failure happened in the service |

## GET /eori/:eori/company-information

An endpoint to retrieve the business full name and address for a given EORI

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

An endpoint to update the verified email address for a given EORI and removes undeliverable information

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

## GET /eori/:eori/eori-history

An endpoint that provides a list of all historic EORI's associated with a given EORI

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

An endpoint that provides XI EORI information for the requested EORI

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

## POST /update-eori-history

An endpoint to populate the historic EORI's for a given EORI

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

An endpoint to update undeliverable information for an enrolmentValue

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

## Running and testing on localhost:
If you want to run [customs-data-store](https://github.com/hmrc/customs-data-store) locally then you also have to run [customs-financials-hods-stub](https://github.com/hmrc/customs-financials-hods-stub) so that it can retrieve historic Eoris from there.  
To start the service from sbt: `sbt "run 9893" ` or from service manager: `sm --start CUSTOMS_DATA_STORE CUSTOMS_FINANCIALS_HODS_STUB -f`  
In Postman
1. Send in any of above requests to http://localhost:9893/customs-data-store/
2. Add and `Authorization` header and set it's value to whatever is in `application.conf  ` under the key `server-token`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## All tests and checks

This is a sbt command alias specific to this project. It will run a scala style check, run unit tests, run integration
tests and produce a coverage report:
> `sbt runAllChecks`
