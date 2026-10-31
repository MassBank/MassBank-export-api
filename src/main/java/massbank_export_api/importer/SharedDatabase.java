package massbank_export_api.importer;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "massbank.shared.enabled", havingValue = "true")
public class SharedDatabase {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final String version;
    private final String commit;

    public SharedDatabase(JdbcTemplate jdbc, EntityManagerFactory entityManagerFactory,
                          @Value("${MB_DATA_VERSION:unknown}") String version,
                          @Value("${MB_DATA_COMMIT:unknown}") String commit) {
        this.dataSource = Objects.requireNonNull(jdbc.getDataSource());
        this.jdbc = jdbc;
        this.version = version;
        this.commit = commit;
    }

    public void prepare() {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM pg_am WHERE amname = 'bingo_idx')
                  AND to_regtype('bingo.sub') IS NOT NULL
                """, Boolean.class))) {
            throw new IllegalStateException("Install Bingo in the shared database before importing records.");
        }
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
        // JPA updates records while persisting their collections. Rebuilding the
        // cartridge index once after bulk loading avoids indexing every update.
        jdbc.execute("DROP INDEX IF EXISTS public.shared_records_bingo_idx");
    }

    public void completed() {
        jdbc.execute("""
                CREATE INDEX shared_records_bingo_idx ON public."massbank-records"
                USING bingo_idx (ch_smiles bingo.molecule) WHERE ch_smiles NOT IN ('', 'N/A')
                """);
        jdbc.execute("ANALYZE public.\"massbank-records\"");
        jdbc.execute("ANALYZE public.\"massbank-peaks\"");
        jdbc.update("UPDATE shared_import_metadata SET ready = true WHERE id = 1");
    }
}
