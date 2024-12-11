FROM eclipse-temurin:21
ARG JAR_FILE=target/massbank3-export-service-*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]