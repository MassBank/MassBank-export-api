package massbank_export_api.importer;

import massbank.AbstractRecord;
import massbank.db.RecordService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchRecordPersister {

    private static final Logger logger = LogManager.getLogger(BatchRecordPersister.class);

    private final RecordService recordService;

    public BatchRecordPersister(RecordService recordService) {
        this.recordService = recordService;
    }

    public PersistOutcome persist(List<AbstractRecord> recordsToPersist) {
        if (recordsToPersist.isEmpty()) {
            return new PersistOutcome(0, 0);
        }

        try {
            final List<AbstractRecord> savedRecords = recordService.saveAll(recordsToPersist);
            final int savedCount = savedRecords == null ? 0 : savedRecords.size();
            final int failedCount = Math.max(recordsToPersist.size() - savedCount, 0);
            if (failedCount > 0) {
                logger.warn("Batch persistence completed with {} failed records.", failedCount);
            }
            return new PersistOutcome(savedCount, failedCount);
        } catch (Exception e) {
            logger.error("Error inserting a record batch of size {}: {}", recordsToPersist.size(), e.getMessage());
            logger.debug("Batch persistence exception details", e);
            return new PersistOutcome(0, recordsToPersist.size());
        }
    }
}


