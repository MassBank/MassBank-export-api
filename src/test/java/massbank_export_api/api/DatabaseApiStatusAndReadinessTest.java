package massbank_export_api.api;

import massbank.db.RecordService;
import massbank_export_api.importer.DataReader;
import massbank_export_api.importer.ImporterConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = org.openapitools.OpenApiGeneratorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ImporterConfiguration.class)
@Testcontainers
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseApiStatusAndReadinessTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataReader dataReader;

    @Autowired
    private RecordService recordService;

    private WebTestClient webTestClient;

    @BeforeAll
    void initClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Nested
    @Order(1)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EmptyDatabaseContext {

        @BeforeAll
        void prepareEmptyDatabase() {
            recordService.deleteAll();
        }

        @Test
        @Order(1)
        void databaseReadyGetReturnsServiceUnavailableWhenNoActiveRecordsExist() {
            webTestClient.get().uri("/database/ready")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody().isEmpty();
        }

        @Test
        @Order(2)
        void databaseStatusGetReturnsNotReadyButReachableWhenNoActiveRecordsExist() {
            webTestClient.get().uri("/database/status")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/json")
                    .expectBody()
                    .jsonPath("$.ready").isEqualTo(false)
                    .jsonPath("$.reachable").isEqualTo(true)
                    .jsonPath("$.active_record_count").isEqualTo(0)
                    .jsonPath("$.message").value(message ->
                            assertTrue(message.toString().contains("no active MassBank records")));
        }
    }

    @Nested
    @Order(2)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LoadedDatabaseContext {

        @BeforeAll
        void prepareLoadedDatabase() {
            recordService.deleteAll();
            loadData();
        }

        @Test
        @Order(1)
        void databaseStatusGetReturnsReadyWhenActiveRecordsExist() {
            webTestClient.get().uri("/database/status")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/json")
                    .expectBody()
                    .jsonPath("$.ready").isEqualTo(true)
                    .jsonPath("$.reachable").isEqualTo(true)
                    .jsonPath("$.active_record_count").value(count ->
                            assertTrue(((Number) count).longValue() > 0, "active_record_count must be > 0"))
                    .jsonPath("$.message").value(message ->
                            assertTrue(message.toString().contains("contains active MassBank records")));
        }

        @Test
        @Order(2)
        void databaseReadyGetReturnsOkAndNoBodyWhenActiveRecordsExist() {
            webTestClient.get().uri("/database/ready")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().isEmpty();
        }
    }

    private void loadData() {
        DataLoadResult result = dataReader.readData();
        assertTrue(result.successful(), result.message());
    }
}







