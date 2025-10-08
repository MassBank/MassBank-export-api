#!/bin/bash

export MB_DATA_DIRECTORY=/MassBank-data
export DB_URL=jdbc:postgresql://localhost:5432/records

su - postgres -c "pg_ctl start -D /var/lib/postgresql/data -l /var/lib/postgresql/log.log"
java -jar app.jar