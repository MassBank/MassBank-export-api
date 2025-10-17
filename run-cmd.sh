#!/bin/bash

# MassBank data directory (default: ./MassBank-data)
MB_DATA_DIRECTORY=${MB_DATA_DIRECTORY:=./MassBank-data}
# Database connection settings (default values)
PGDATABASE=${PGDATABASE:=postgres}
PGUSER_EXPORT_API=${PGUSER_EXPORT_API:=massbank_export_api_user}
PGPASSWORD_EXPORT_API=${PGPASSWORD_EXPORT_API:=massbank-export-api-password}
PGDATABASE_EXPORT_API=${PGDATABASE_EXPORT_API:=records}

PGDATABASE=$PGDATABASE psql -c "DROP DATABASE $PGDATABASE_EXPORT_API WITH (FORCE);"
PGDATABASE=$PGDATABASE psql -c "DROP USER $PGUSER_EXPORT_API;"
sh initDB.sh
mvn clean package -DskipTests
# mvn clean package
java -jar target/massbank-export-api-*.jar