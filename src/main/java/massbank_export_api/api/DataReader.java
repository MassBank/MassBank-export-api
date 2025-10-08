package massbank_export_api.api;

import massbank.Record;
import massbank.RecordParser;
import massbank_export_api.api.db.DbRecord;
import massbank_export_api.api.db.RecordServiceImplementation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petitparser.context.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
@EnableJpaRepositories(basePackages = "massbank_export_api.api.db")
@EntityScan(basePackages = "massbank_export_api.api.db")
@EnableAutoConfiguration
public class DataReader {

    private static final Logger logger = LogManager.getLogger(DataReader.class);

    @Value("${MB_DATA_DIRECTORY}")
    public String dataDirectory;

    private final RecordServiceImplementation recordServiceImplementation;

    @Autowired
    public DataReader(RecordServiceImplementation recordServiceImplementation) {
        this.recordServiceImplementation = recordServiceImplementation;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void readDataAfterStartup() {
        logger.info("Application started with data directory: {}", dataDirectory);

        recordServiceImplementation.deleteAll();
        logger.info("Cleared existing records in the database.");

        final Path dataDirectoryPath = Paths.get(dataDirectory);
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + dataDirectoryPath + "/*/*.txt");
        final AtomicInteger progressCounter = new AtomicInteger(0);
        final RecordParser recordparser = new RecordParser(new HashSet<>());

        try {
            recordServiceImplementation.deleteAll();
            final long initialCount = recordServiceImplementation.count();
            logger.info("Database connectivity test successful. Initial record count: {}", initialCount);
        } catch (Exception e) {
            logger.error("Database connectivity test failed", e);
            logger.error(
                    "Cannot proceed with database mode. Check your database configuration and connectivity.");
            return;
        }

        if (Files.exists(dataDirectoryPath) && Files.isDirectory(dataDirectoryPath)) {
            try (Stream<Path> paths = Files.walk(dataDirectoryPath)
                    .filter(Files::isRegularFile)
                    .filter(matcher::matches)) {
                final List<Path> recordFiles = paths.toList();
                logger.info("Found {} record files in the directory: {}", recordFiles.size(), dataDirectory);
                final int totalRecords = recordFiles.size();

                recordFiles.parallelStream().forEach(filename -> {
                    try {
                        final String content = Files.readString(filename, StandardCharsets.UTF_8);
                        final Result result = recordparser.parse(content);
                        if (result.isSuccess()) {
                            final Record record = result.get();
                            if (!record.isDeprecated()) {
                                final String accession = record.ACCESSION();

                                try {
                                    final DbRecord dbRecord = new DbRecord(null, accession, content);
                                    final DbRecord savedRecord = recordServiceImplementation.insert(dbRecord);
                                    if (savedRecord != null && savedRecord.getId() != null) {
                                        logger.debug(
                                                "Successfully inserted record with accession: {} and ID: {}",
                                                accession, savedRecord.getId());
                                    } else {
                                        logger.error(
                                                "Failed to insert record with accession: {} - saved record is null or has no ID",
                                                accession);
                                    }
                                } catch (Exception e) {
                                    logger.error("Error inserting record with accession: {}", accession, e);
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error reading file: {}", filename, e);
                    }
                    final int progress = progressCounter.incrementAndGet();
                    if (progress % (totalRecords / 10) == 0) {
                        logger.info("Progress: {}/{}", progress, totalRecords);
                    }
                });

                logger.info("Data loading completed in database mode.");
                try {
                    final long finalCount = recordServiceImplementation.count();
                    logger.info("Final count of records in database: {}", finalCount);
                    if (finalCount == 0) {
                        logger.warn(
                                "No records were stored in the database. Check database connectivity and transaction settings.");
                    }
                } catch (Exception e) {
                    logger.error("Error getting final count from database", e);
                }
            } catch (IOException e) {
                logger.error("Error finding record files in data directory", e);
            }
        } else {
            logger.error("The specified directory does not exist or is not a directory: {}", dataDirectory);
        }
    }
}