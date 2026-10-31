package massbank_export_api.importer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "shared_import_metadata")
public class ImportMetadata {
    @Id
    private Long id = 1L;
    @Column(name = "git_commit", nullable = false)
    private String gitCommit = "";
    @Column(name = "data_version", nullable = false)
    private String dataVersion = "";
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt = Instant.now();
    @Column(nullable = false)
    private boolean ready;
}
