package massbank_export_api.api;

import massbank.db.RecordService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/database")
public class DatabaseStatusController {

    private static final Logger logger = LogManager.getLogger(DatabaseStatusController.class);

    private final RecordService recordService;

    public DatabaseStatusController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseStatus> status() {
        return ResponseEntity.ok(checkDatabaseStatus());
    }

    @GetMapping(value = "/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseStatus> ready() {
        final DatabaseStatus status = checkDatabaseStatus();
        return ResponseEntity.status(status.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(status);
    }

    private DatabaseStatus checkDatabaseStatus() {
        try {
            final long activeRecordCount = recordService.countActive();
            final boolean ready = activeRecordCount > 0;
            final String message = ready
                    ? "Database is reachable and contains active MassBank records."
                    : "Database is reachable, but no active MassBank records are available.";
            return new DatabaseStatus(ready, true, activeRecordCount, message);
        } catch (Exception e) {
            logger.warn("Database readiness check failed", e);
            return new DatabaseStatus(false, false, null,
                    "Database is not reachable: " + e.getClass().getSimpleName());
        }
    }
}
