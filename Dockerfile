FROM postgres:18-alpine
WORKDIR /app

COPY entrypoint.sh entrypoint.sh
COPY initDB.sh initDB.sh

RUN (addgroup -S postgres && adduser -S postgres -G postgres || true) && \
    rm -rf /var/lib/postgresql/data && \
    mkdir -p /var/lib/postgresql/data && \
    chmod -R 777 /var/lib/postgresql/data && \ 
    chown -R postgres:postgres /var/lib/postgresql/data && \
    su - postgres -c "initdb /var/lib/postgresql/data" && \ 
    su - postgres -c "pg_ctl start -D /var/lib/postgresql/data -l /var/lib/postgresql/log.log" && \
    cat initDB.sh | su - postgres -c "sh" && \
    su - postgres -c "pg_ctl stop -D /var/lib/postgresql/data"

RUN apk add openjdk21

ARG JAR_FILE=target/massbank-export-api-*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["sh", "entrypoint.sh"]
EXPOSE 8080