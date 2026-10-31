package massbank_export_api.importer;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@ConditionalOnProperty(name = "massbank.shared.enabled", havingValue = "true")
public class SharedDatabase {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final String version;
    private final String commit;

    public SharedDatabase(DataSource dataSource, EntityManagerFactory entityManagerFactory,
                          @Value("${MB_DATA_VERSION:unknown}") String version,
                          @Value("${MB_DATA_COMMIT:unknown}") String commit) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.version = version;
        this.commit = commit;
    }

    public void prepare() {
        // JPA creates the source tables first; SQL only supplies read views and
        // PostgreSQL-specific indexes that cannot be expressed with JPA.
        new ResourceDatabasePopulator(new ClassPathResource("shared-api.sql")).execute(dataSource);
    }

    public void starting() {
        jdbc.update("""
                INSERT INTO shared_import_metadata (id, git_commit, data_version, imported_at, ready)
                VALUES (1, ?, ?, CURRENT_TIMESTAMP, false)
                ON CONFLICT (id) DO UPDATE SET git_commit = EXCLUDED.git_commit,
                  data_version = EXCLUDED.data_version, imported_at = EXCLUDED.imported_at, ready = false
                """, commit, version);
    }

    public void completed() {
        jdbc.execute("ANALYZE public.\"massbank-records\"");
        jdbc.execute("ANALYZE public.\"massbank-peaks\"");
        jdbc.update("UPDATE shared_import_metadata SET ready = true WHERE id = 1");
    }
}
