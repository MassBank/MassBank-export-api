package massbank_export_api.importer;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedDatabaseTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private SharedDatabase database() {
        when(jdbc.getDataSource()).thenReturn(mock(DataSource.class));
        return new SharedDatabase(jdbc, mock(EntityManagerFactory.class), "test", "commit");
    }

    @Test
    void missingBingoFailsBeforeChangingTheDataset() {
        SharedDatabase database = database();
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        assertThrows(IllegalStateException.class, database::prepare);
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void disablesReadinessBeforeDroppingTheOldIndex() {
        database().starting();
        InOrder order = inOrder(jdbc);
        order.verify(jdbc).update(contains("ready = false"), eq("commit"), eq("test"));
        order.verify(jdbc).execute("DROP INDEX IF EXISTS public.shared_records_bingo_idx");
    }

    @Test
    void onlyMarksReadyAfterBuildingTheBingoIndex() {
        database().completed();
        InOrder order = inOrder(jdbc);
        order.verify(jdbc).execute(contains("CREATE INDEX shared_records_bingo_idx"));
        order.verify(jdbc).execute("ANALYZE public.\"massbank-records\"");
        order.verify(jdbc).execute("ANALYZE public.\"massbank-peaks\"");
        order.verify(jdbc).update("UPDATE shared_import_metadata SET ready = true WHERE id = 1");
    }

    @Test
    void indexFailureNeverMarksTheDatabaseReady() {
        SharedDatabase database = database();
        doThrow(new IllegalStateException("index failed")).when(jdbc).execute(contains("CREATE INDEX"));
        assertThrows(IllegalStateException.class, database::completed);
        verify(jdbc, never()).update("UPDATE shared_import_metadata SET ready = true WHERE id = 1");
    }
}
