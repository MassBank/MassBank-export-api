#!/bin/bash

# set MB_DATA_DIRECTORY and DB_URL environment variables beforehand if not already done
# export MB_DATA_DIRECTORY=/MassBank-data
# export DB_URL=jdbc:postgresql://localhost:5432/records

psql postgres -c "DROP DATABASE records WITH (FORCE);" && \
psql postgres -c "DROP USER massbank;" && \
sh initDB.sh && \
# mvn clean package -DskipTests && \
mvn clean package && \
java -jar target/massbank-export-api-*.jar