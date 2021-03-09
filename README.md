
# customs-data-store

This repository contains the code for a persistent cache holding customs related data.

| Path                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| GET /customs-data-store/eori/:eori/verified-email | Retrieve the verified email address for a given EORI either from the cache or SUB09|
| GET /customs-data-store/eori/:eori/eori-history | Retrieves the historic eori's for a given EORI either from the cache or SUB21        |
| POST /customs-data-store/update-email | Populates a new verified email address in the cache | 
| POST /customs-data-store/update-eori-history | Updates the eori history for a given EORI in the cache |

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

## POST /update-email

An endpoint to update the verified email address for a given EORI

### Example request

```json
{
  "eori" : "GB333186811511",
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
    "eori": "GB333186811511", 
    "validFrom": "2001-01-20T00:00:00Z", 
    "validTo": "2001-01-20T00:00:00Z"
  },
  {
    "eori": "GB333186811531",
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

An endpoint to populate the historic EORI's for a given EORI

### Example request

```json
{
  "eori" : "GB333186811511"
}
```

### Response codes

| Status                               | Description                                          |
| ---------------------------------  | ---------------------------------------------------- |
| 204 | Successfully updated the historic EORI's in the cache       |
| 500 | An unexpected failure happened in the service |




## Running and testing on localhost:
If you want to run [customs-data-store](https://github.com/hmrc/customs-data-store) locally then you also have to run [customs-financials-hods-stub](https://github.com/hmrc/customs-financials-hods-stub) so that it can retrieve historic Eoris from there.  
To start the service from sbt: `sbt "run 9893" ` or from service manager: `sm --start CUSTOMS_DATA_STORE CUSTOMS_FINANCIALS_HODS_STUB -f`  
In Postman
1. Send in any of above requests to http://localhost:9893/customs-data-store/
2. Add and `Authorization` header and set it's value to whatever is in `application.conf  ` under the key `server-token`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
