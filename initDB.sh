#!/bin/bash

psql postgres -c "CREATE USER massbank WITH ENCRYPTED PASSWORD 'massbank-password';" && \
psql postgres -c "CREATE DATABASE records OWNER massbank;" && \
psql -U massbank -d records -c "CREATE TABLE record (id SERIAL PRIMARY KEY, accession VARCHAR(50) UNIQUE NOT NULL, content TEXT NOT NULL);"