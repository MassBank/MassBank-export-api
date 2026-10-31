package massbank_export_api.importer;

import massbank.db.RecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DataReaderInputTest {
    @TempDir
    Path directory;

    @Test
    void missingOrEmptyInputNeverResetsTheSharedDatabase() {
        RecordService records = mock(RecordService.class);
        RecordImportPipeline pipeline = mock(RecordImportPipeline.class);
        DataReader reader = new DataReader(records, pipeline,
                new StaticListableBeanFactory().getBeanProvider(SharedDatabase.class));
        reader.dataDirectory = directory.resolve("missing").toString();
        assertFalse(reader.readData().successful());
        reader.dataDirectory = directory.toString();
        assertFalse(reader.readData().successful());
        verifyNoInteractions(records, pipeline);
    }
}
