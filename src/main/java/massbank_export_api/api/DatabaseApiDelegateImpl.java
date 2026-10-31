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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

@Primary
@Service
public class DatabaseApiDelegateImpl implements DatabaseApiDelegate {

    private static final Logger logger = LogManager.getLogger(DatabaseApiDelegateImpl.class);

    private final RecordService recordService;
    private final JdbcTemplate jdbc;
    private final boolean shared;

    public DatabaseApiDelegateImpl(RecordService recordService, JdbcTemplate jdbc,
                                  @Value("${massbank.shared.enabled:false}") boolean shared) {
        this.recordService = recordService;
        this.jdbc = jdbc;
        this.shared = shared;
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
            return recordService.countActive() > 0 && importCompleted();
        } catch (Exception e) {
            logger.warn("Database readiness check failed", e);
            return false;
        }
    }

    private DatabaseStatus checkDatabaseStatus() {
        try {
            final long activeRecordCount = recordService.countActive();
            final boolean ready = activeRecordCount > 0 && importCompleted();
            final String message = ready
                    ? "Database is reachable and contains active MassBank records."
                    : "Database is reachable, but no active MassBank records are available or the import is incomplete.";
            return new DatabaseStatus(ready, true, message)
                    .activeRecordCount(activeRecordCount);
        } catch (Exception e) {
            logger.warn("Database readiness check failed", e);
            return new DatabaseStatus(false, false,
                    "Database is not reachable: " + e.getClass().getSimpleName());
        }

    }

    private boolean importCompleted() {
        return !shared || Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT coalesce((SELECT ready FROM shared_import_metadata WHERE id = 1), false)", Boolean.class));
    }
}
