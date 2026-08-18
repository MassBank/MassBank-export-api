package massbank_export_api.importer;

import massbank.AbstractRecord;
import massbank.Record;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RecordImportPipeline {

    private static final Logger logger = LogManager.getLogger(RecordImportPipeline.class);

    private final int producerThreads;
    private final int queueCapacity;
    private final int chunkSize;
    private final BatchRecordPersister batchRecordPersister;
    private final RecordFileProducer recordFileProducer;

    public RecordImportPipeline(
            @Value("${import.producer-threads:8}") int producerThreads,
            @Value("${import.queue-capacity:10000}") int queueCapacity,
            @Value("${import.chunk-size:2000}") int chunkSize,
            BatchRecordPersister batchRecordPersister,
            RecordFileProducer recordFileProducer) {
        this.producerThreads = Math.max(1, producerThreads);
        this.queueCapacity = Math.max(1, queueCapacity);
        this.chunkSize = Math.max(1, chunkSize);
        this.batchRecordPersister = batchRecordPersister;
        this.recordFileProducer = recordFileProducer;
    }

    public PipelineOutcome run(List<Path> recordFiles) {
        final int totalRecords = recordFiles.size();
        final int progressStep = Math.max(totalRecords / 10, 1);
        final int effectiveChunkSize = chunkSize;
        final int effectiveQueueCapacity = Math.max(effectiveChunkSize, queueCapacity);

        logger.info(
                "Import pipeline configuration: producerThreads={}, queueCapacity={}, chunkSize={}",
                producerThreads,
                effectiveQueueCapacity,
                effectiveChunkSize);

        final BlockingQueue<Optional<Record>> queue = new LinkedBlockingQueue<>(effectiveQueueCapacity);
        final AtomicInteger progressCounter = new AtomicInteger(0);
        final AtomicInteger savedCounter = new AtomicInteger(0);
        final AtomicInteger failedCounter = new AtomicInteger(0);
        final AtomicReference<Throwable> fatalError = new AtomicReference<>();

        final ExecutorService producerExecutor = Executors.newFixedThreadPool(producerThreads);
        final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();

        final Future<?> consumerFuture = consumerExecutor.submit(
                () -> consume(queue, effectiveChunkSize, savedCounter, failedCounter, fatalError));

        final List<Future<?>> producerFutures = new ArrayList<>(recordFiles.size());
        for (Path filename : recordFiles) {
            producerFutures.add(producerExecutor.submit(
                    () -> {
                        try {
                            recordFileProducer.produce(filename, queue, failedCounter, fatalError);
                        } finally {
                            final int progress = progressCounter.incrementAndGet();
                            if (progress % progressStep == 0 || progress == totalRecords) {
                                logger.info("Progress: {}/{}", progress, totalRecords);
                            }
                        }
                    }));
        }

        for (Future<?> producerFuture : producerFutures) {
            try {
                producerFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fatalError.compareAndSet(null, e);
                break;
            } catch (ExecutionException e) {
                fatalError.compareAndSet(null, e.getCause() == null ? e : e.getCause());
            }
        }

        try {
            queue.put(Optional.empty());
            consumerFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fatalError.compareAndSet(null, e);
        } catch (ExecutionException e) {
            fatalError.compareAndSet(null, e.getCause() == null ? e : e.getCause());
        } finally {
            producerExecutor.shutdownNow();
            consumerExecutor.shutdownNow();
            awaitExecutorShutdown(producerExecutor, "producer");
            awaitExecutorShutdown(consumerExecutor, "consumer");
        }

        if (fatalError.get() != null) {
            failedCounter.incrementAndGet();
            logger.error("Fatal error while executing the import pipeline.", fatalError.get());
        }

        return new PipelineOutcome(savedCounter.get(), failedCounter.get(), fatalError.get());
    }

    private void consume(
            BlockingQueue<Optional<Record>> queue,
            int chunkSize,
            AtomicInteger savedCounter,
            AtomicInteger failedCounter,
            AtomicReference<Throwable> fatalError) {
        final List<AbstractRecord> batch = new ArrayList<>(chunkSize);
        try {
            while (true) {
                final Optional<Record> next = queue.take();
                if (next.isEmpty()) {
                    break;
                }
                batch.add(next.get());
                if (batch.size() >= chunkSize) {
                    flushBatch(batch, savedCounter, failedCounter);
                }
            }
            flushBatch(batch, savedCounter, failedCounter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fatalError.compareAndSet(null, e);
        }
    }

    private void flushBatch(List<AbstractRecord> batch, AtomicInteger savedCounter, AtomicInteger failedCounter) {
        if (batch.isEmpty()) {
            return;
        }
        final PersistOutcome outcome = batchRecordPersister.persist(List.copyOf(batch));
        batch.clear();
        savedCounter.addAndGet(outcome.saved());
        failedCounter.addAndGet(outcome.failed());
    }

    private void awaitExecutorShutdown(ExecutorService executorService, String name) {
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("{} executor did not terminate within timeout.", name);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for {} executor shutdown.", name);
        }
    }
}





