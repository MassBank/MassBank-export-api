#!/bin/bash

mvn clean package && \
docker build -t 'massbank-export-api' . && \
docker run -d --name massbank-export-api -p 8080:8080 -v $(pwd)/MassBank-data:/MassBank-data massbank-export-api