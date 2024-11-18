package de.ipb_halle.massbank3_export_service.api;

import massbank.RecordParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petitparser.context.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class DataReader {

    private static final Logger logger = LogManager.getLogger(DataReader.class);

    @Value("${MB_DATA_DIRECTORY}")
    private String dataDirectory;

    @EventListener(ApplicationReadyEvent.class)
    public void readDataAfterStartup() {
        logger.info("Application started with data directory: {}", dataDirectory);
        Path dataDirectoryPath = Paths.get(dataDirectory);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + dataDirectoryPath + "/*/*.txt");
        AtomicInteger progressCounter = new AtomicInteger(0);
        RecordParser recordparser = new RecordParser(Set.of("legacy"));

        if (Files.exists(dataDirectoryPath) && Files.isDirectory(dataDirectoryPath)) {
            try (Stream<Path> paths = Files.walk(dataDirectoryPath)
                .filter(Files::isRegularFile)
                .filter(matcher::matches)) {
                List<Path> recordFiles = paths.toList();
                logger.info("Found {} record files in the directory", recordFiles.size());
                int totalRecords = recordFiles.size();

                long recordCount = recordFiles.parallelStream()
                    .map(filename -> {
                        try {
                            return Files.readString(filename, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            logger.error("Error reading file: {}", filename, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(recordparser::parse)
                    .filter(Result::isSuccess)
                    .map(Result::get)
                    .peek(record -> {
                        int progress = progressCounter.incrementAndGet();
                        if (progress % (totalRecords / 10) == 0) {
                            logger.info("Progress: {}/{}", progress, totalRecords);
                        }
                    })
                    .count();
                logger.info("Read {} records", recordCount);
            } catch (IOException e) {
                logger.error("Error finding record files in data directory", e);
            }
        } else {
            logger.error("The specified directory does not exist or is not a directory: {}", dataDirectory);
        }
    }
}