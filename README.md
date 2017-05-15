# play-elastic-govdata
PoC for reading / writing data.gov.uk datasets with Play

## Run

`sbt run`

## Connect

`http://localhost:9000/hospital/cross` runs a wildcard `*cross*` query on hospital names, against a remote Elasticsearch instance