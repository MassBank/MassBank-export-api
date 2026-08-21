FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .openapi-generator-ignore ./
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /
COPY --from=build /workspace/target/massbank-export-api-rest-api.jar /app-rest.jar
COPY --from=build /workspace/target/massbank-export-api-data-import.jar /app-import.jar
ENTRYPOINT ["java", "-Xmx1200m", "-jar", "/app-rest.jar"]
