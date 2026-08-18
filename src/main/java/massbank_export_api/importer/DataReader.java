package massbank_export_api.importer;

import massbank.db.RecordService;
import massbank_export_api.api.DataLoadResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DataReader {

    private static final Logger logger = LogManager.getLogger(DataReader.class);

    @Value("${DATA_DIRECTORY}")
    public String dataDirectory;

    private final RecordService recordService;
    private final RecordImportPipeline recordImportPipeline;

    @Autowired
    public DataReader(RecordService recordService, RecordImportPipeline recordImportPipeline) {
        this.recordService = recordService;
        this.recordImportPipeline = recordImportPipeline;
    }

    public DataLoadResult readData() {
        logger.info("Starting MassBank data import from directory: {}", dataDirectory);

        final Path dataDirectoryPath = Paths.get(dataDirectory);
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + dataDirectoryPath + "/*/*.txt");
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
                final PipelineOutcome pipelineOutcome = recordImportPipeline.run(recordFiles);

                logger.info("Data loading completed in database mode.");
                try {
                    final long finalCount = recordService.countActive();
                    logger.info("Final count of records in database: {}", finalCount);
                    if (finalCount == 0) {
                        logger.warn(
                                "No records were stored in the database. Check database connectivity and transaction settings.");
                    }
                    final boolean successful = finalCount > 0
                            && pipelineOutcome.failedRecords() == 0
                            && !pipelineOutcome.hasFatalError();
                    final String message = successful
                            ? "Data loading completed successfully."
                            : "Data loading completed, but the database is not ready.";
                    return new DataLoadResult(totalRecords, pipelineOutcome.savedRecords(), pipelineOutcome.failedRecords(),
                            finalCount, successful, message);
                } catch (Exception e) {
                    logger.error("Error getting final count from database", e);
                    return new DataLoadResult(totalRecords, pipelineOutcome.savedRecords(), pipelineOutcome.failedRecords(),
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

