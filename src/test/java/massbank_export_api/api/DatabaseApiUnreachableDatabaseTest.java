package massbank_export_api.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

@SpringBootTest(
        classes = org.openapitools.OpenApiGeneratorApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/massbank",
                "spring.datasource.username=broken",
                "spring.datasource.password=broken",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.hikari.initializationFailTimeout=0",
                "spring.datasource.hikari.connectionTimeout=1000",
                "spring.datasource.hikari.validationTimeout=1000",
                "spring.sql.init.mode=never",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
        }
)
class DatabaseApiUnreachableDatabaseTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void initClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Test
    void databaseReadyGetReturnsInternalServerErrorWhenDatabaseIsUnreachable() {
        webTestClient.get().uri("/database/ready")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.error").isEqualTo("Internal Server Error")
                .jsonPath("$.path").isEqualTo("/database/ready");
    }

    @Test
    void databaseStatusGetReturnsInternalServerErrorWhenDatabaseIsUnreachable() {
        webTestClient.get().uri("/database/status")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.error").isEqualTo("Internal Server Error")
                .jsonPath("$.path").isEqualTo("/database/status");
    }
}




