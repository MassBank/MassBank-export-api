#!/bin/bash

MB_DATA_DIRECTORY=${MB_DATA_DIRECTORY:=./MassBank-data}

mvn clean package -DskipTests && \
docker build -t 'massbank-export-api' . && \
docker run -d --name massbank-export-api -p 8080:8080 -v $MB_DATA_DIRECTORY:/MassBank-data massbank-export-api