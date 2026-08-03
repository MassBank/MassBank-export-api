package massbank_export_api.api;

import massbank.AbstractRecord;
import massbank.Record;
import massbank.RecordParser;
import massbank.db.RecordService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petitparser.context.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class DataReader {

    private static final Logger logger = LogManager.getLogger(DataReader.class);

    @Value("${DATA_DIRECTORY}")
    public String dataDirectory;

    private final RecordService recordService;

    @Autowired
    public DataReader(RecordService recordService) {
        this.recordService = recordService;
    }

    public DataLoadResult readData() {
        logger.info("Starting MassBank data import from directory: {}", dataDirectory);

        final Path dataDirectoryPath = Paths.get(dataDirectory);
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + dataDirectoryPath + "/*/*.txt");
        final AtomicInteger progressCounter = new AtomicInteger(0);
        final AtomicInteger savedRecordsCounter = new AtomicInteger(0);
        final AtomicInteger failedRecordsCounter = new AtomicInteger(0);
        try {
            recordService.deleteAll();
            logger.info("Database connectivity test successful. Cleared existing records in the database.");
        } catch (Exception e) {
            logger.error("Database connectivity test failed", e);
            logger.error("Check your database configuration and connectivity.");
            return new DataLoadResult(0, 0, 0, 0, false, "Database connectivity test failed.");
        }

        if (Files.exists(dataDirectoryPath) && Files.isDirectory(dataDirectoryPath)) {
            try (Stream<Path> paths = Files.walk(dataDirectoryPath)
                    .filter(Files::isRegularFile)
                    .filter(matcher::matches)) {
                final List<Path> recordFiles = paths.toList();
                logger.info("Found {} record files in the directory: {}", recordFiles.size(), dataDirectory);
                final int totalRecords = recordFiles.size();
                if (totalRecords == 0) {
                    return new DataLoadResult(0, 0, 0, 0, false,
                            "No record files were found in the configured data directory.");
                }
                final int progressStep = Math.max(totalRecords / 10, 1);

                recordFiles.parallelStream().forEach(filename -> {
                    try {
                        final String content = Files.readString(filename, StandardCharsets.UTF_8);
                        final RecordParser recordparser = new RecordParser(new HashSet<>());
                        final Result result = recordparser.parse(content);
                        if (result.isSuccess()) {
                            final AbstractRecord record = result.get();
                            if (record instanceof Record typedRecord) {
                                final String accession = typedRecord.getAccession();
                                try {
                                    final AbstractRecord savedRecord = recordService.save(typedRecord);
                                    if (savedRecord != null) {
                                        savedRecordsCounter.incrementAndGet();
                                        logger.debug("Successfully inserted record with accession: {}", accession);
                                    } else {
                                        failedRecordsCounter.incrementAndGet();
                                        logger.error("Failed to insert record with accession: {} - saved record is null",
                                                accession);
                                    }
                                } catch (Exception e) {
                                    failedRecordsCounter.incrementAndGet();
                                    logger.error("Error inserting record with accession: {}", accession, e);
                                }
                            } else {
                                failedRecordsCounter.incrementAndGet();
                                logger.warn("Parsed file did not contain a MassBank record: {}", filename);
                            }
                        } else {
                            failedRecordsCounter.incrementAndGet();
                            logger.warn("Could not parse MassBank record file: {}", filename);
                        }
                    } catch (IOException e) {
                        failedRecordsCounter.incrementAndGet();
                        logger.error("Error reading file: {}", filename, e);
                    }
                    final int progress = progressCounter.incrementAndGet();
                    if (progress % progressStep == 0 || progress == totalRecords) {
                        logger.info("Progress: {}/{}", progress, totalRecords);
                    }
                });

                logger.info("Data loading completed in database mode.");
                try {
                    final long finalCount = recordService.countActive();
                    logger.info("Final count of records in database: {}", finalCount);
                    if (finalCount == 0) {
                        logger.warn(
                                "No records were stored in the database. Check database connectivity and transaction settings.");
                    }
                    final boolean successful = finalCount > 0 && failedRecordsCounter.get() == 0;
                    final String message = successful
                            ? "Data loading completed successfully."
                            : "Data loading completed, but the database is not ready.";
                    return new DataLoadResult(totalRecords, savedRecordsCounter.get(), failedRecordsCounter.get(),
                            finalCount, successful, message);
                } catch (Exception e) {
                    logger.error("Error getting final count from database", e);
                    return new DataLoadResult(totalRecords, savedRecordsCounter.get(), failedRecordsCounter.get(),
                            0, false, "Error getting final count from database.");
                }
            } catch (IOException e) {
                logger.error("Error finding record files in data directory", e);
                return new DataLoadResult(0, 0, 0, 0, false, "Error finding record files in data directory.");
            }
        } else {
            logger.error("The specified directory does not exist or is not a directory: {}", dataDirectory);
            return new DataLoadResult(0, 0, 0, 0, false,
                    "The specified data directory does not exist or is not a directory.");
        }
    }
}