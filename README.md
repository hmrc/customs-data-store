
# customs-data-store

This repository contains the code for a persistent cache holding customs related data.
It uses graphql queries for querying, updating and inserting.
 
### Running and testing on localhost:  
If you want to run [customs-data-store](https://github.com/hmrc/customs-data-store) locally then you also have to run [customs-financials-hods-stub](https://github.com/hmrc/customs-financials-hods-stub) so that it can retrieve historic Eoris from there.  
To start the service from sbt: `sbt "run 9893" ` or from service manager: `sm --start CUSTOMS_DATA_STORE CUSTOMS_FINANCIALS_HODS_STUB -f`  
In Postman
1. Create a **POST** request to http://localhost:9893/customs-data-store/graphql
2. Add and `Authorization` header and set it's value to whatever is in `application.conf  ` under the key `server-token`
3. Send in any of the requests in the following section to play around. If you want to retrieve actual data, you should insert it first (mutation).

### GraphQL examples

##### Example queries for retrieveing data (Select):

Will return the values held in 'address' only for the given EORI.
```json
{ "query": "query { byEori( eori: \"GB12345678\") { notificationEmail { address }  } }"}
```

Will return the values held in 'address' and 'timestamp' for a given EORI.
```json
{ "query": "query { byEori( eori: \"GB12345678\") { notificationEmail { address, timestamp } } }"}
```

Will return the 'eori','validFrom','validUntil','address' and 'timestamp' for a given EORI.
```json
{ "query": "query { byEori( eori: \"GB12345678\") {eoriHistory {eori validFrom validUntil},  notificationEmail { address, timestamp } } }"}
```

##### Example queries for upserting data (Insert/Update)

Updating/Inserting the dates on an EORI or inserting it; without an email:
```json
{"query" : "mutation {byEori(eoriHistory:{eori:\"GB12345678\" validFrom:\"20180101\" validUntil:\"20200101\"} )}" }
```

Updating/Inserting an EORI with an email and timestamp:
```json
{"query" : "mutation {byEori(eoriHistory:{eori:\"EORI11223344\" validFrom:\"20180101\" validUntil:\"20200101\"}, notificationEmail: {address: \"someemail@mail.com\", timestamp: \"timestamp\"} )}" }
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
