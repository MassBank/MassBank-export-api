package massbank_export_api.importer;

import massbank.Record;
import massbank.RecordParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.petitparser.context.Result;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RecordFileProducer {

    private static final Logger logger = LogManager.getLogger(RecordFileProducer.class);

    public void produce(
            Path filename,
            BlockingQueue<Optional<Record>> queue,
            AtomicInteger failedCounter,
            AtomicReference<Throwable> fatalError) {
        try {
            final String content = Files.readString(filename, StandardCharsets.UTF_8);
            final HashSet<String> configKeys = new HashSet<>();
            configKeys.add("legacy");
            final Result result = new RecordParser(configKeys).parse(content);
            if (result.isSuccess() && result.get() instanceof Record record) {
                queue.put(Optional.of(record));
            } else {
                failedCounter.incrementAndGet();
                logger.warn("Could not parse MassBank record file: {}", filename);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fatalError.compareAndSet(null, e);
        } catch (IOException e) {
            failedCounter.incrementAndGet();
            logger.error("Error reading file: {}", filename, e);
        } catch (Exception e) {
            failedCounter.incrementAndGet();
            logger.error("Unexpected error while producing record from file: {}", filename, e);
        }
    }
}

