# Hinweise für zukünftige Sessions

Dieses Repository enthält eine Spring-Boot-REST-API für MassBank-Datensätze und eine separate Importanwendung. Beide werden aus demselben Maven-Projekt gebaut.

## Orientierung

- `src/main/resources/openapi.yaml` definiert den API-Vertrag. Änderungen an Endpunkten und Modellen hier beginnen und die handgeschriebene Implementierung sowie die betroffenen Tests mitziehen. Generierten Code unter `target/` nicht direkt bearbeiten.
- `src/main/java/massbank_export_api/api/` enthält die API-Implementierung; `src/main/java/massbank_export_api/importer/` enthält den Datenimport.
- `pom.xml` konfiguriert die OpenAPI-Codegenerierung und baut getrennte JARs mit den Klassifizierern `rest-api` und `data-import`.
- `README.md` ist die Anleitung für Entwicklung und Betrieb. Bei Änderungen an Startbefehlen, Umgebungsvariablen oder Container-Konfiguration die README mit `Dockerfile`, `docker-compose.yml` und `env.dist` abgleichen.

## Build und Tests

- Java 25 verwenden (siehe `pom.xml`). Der dokumentierte Build-Befehl ist `mvn clean package`.
- Tests liegen unter `src/test/java/`. Für gezielte Prüfungen `mvn -Dtest=<Testklasse> test` verwenden, für den gesamten Testlauf `mvn test`.
- Mehrere API- und Import-Integrationstests verwenden Testcontainers. Vor dem Ausführen sicherstellen, dass eine kompatible Container-Laufzeit erreichbar ist; Hinweise zu Podman stehen in der README.