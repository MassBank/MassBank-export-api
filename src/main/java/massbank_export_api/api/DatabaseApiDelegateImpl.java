package massbank_export_api.api;

import massbank.db.RecordService;
import massbank_export_api.model.DatabaseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service
public class DatabaseApiDelegateImpl implements DatabaseApiDelegate {

    private static final Logger logger = LogManager.getLogger(DatabaseApiDelegateImpl.class);

    private final RecordService recordService;

    public DatabaseApiDelegateImpl(RecordService recordService) {
        this.recordService = recordService;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<DatabaseStatus> databaseStatusGet() {
        return ResponseEntity.ok(checkDatabaseStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Void> databaseReadyGet() {
        return isDatabaseReady()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    private boolean isDatabaseReady() {
        try {
            return recordService.countActive() > 0;
        } catch (Exception e) {
            logger.warn("Database readiness check failed", e);
            return false;
        }
    }

    private DatabaseStatus checkDatabaseStatus() {
        try {
            final long activeRecordCount = recordService.countActive();
            final boolean ready = activeRecordCount > 0;
            final String message = ready
                    ? "Database is reachable and contains active MassBank records."
                    : "Database is reachable, but no active MassBank records are available.";
            return new DatabaseStatus(ready, true, message)
                    .activeRecordCount(activeRecordCount);
        } catch (Exception e) {
            logger.warn("Database readiness check failed", e);
            return new DatabaseStatus(false, false,
                    "Database is not reachable: " + e.getClass().getSimpleName());
        }
    }
}


