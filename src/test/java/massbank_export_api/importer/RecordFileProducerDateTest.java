package massbank_export_api.importer;

import massbank.AbstractRecord;
import massbank.Record;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RecordFileProducerDateTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = {
            "2012.10.22 (Created 2008.05.12)",
            "2016.01.19 (Created 2006.12.21, modified 2011.05.06)",
            "2025.01.17"
    })
    void preservesTheCompleteSourceDate(String date) throws Exception {
        String fixture = Files.readString(Path.of(
                "src/test/resources/MassBank-data-test/IPB_Halle/MSBNK-IPB_Halle-PB001341.txt"),
                StandardCharsets.UTF_8);
        Path file = directory.resolve("MSBNK-IPB_Halle-PB001341.txt");
        Files.writeString(file, fixture.replaceFirst("(?m)^DATE: .*", "DATE: " + date),
                StandardCharsets.UTF_8);
        var queue = new LinkedBlockingQueue<Optional<AbstractRecord>>();
        var failures = new AtomicInteger();
        var fatal = new AtomicReference<Throwable>();

        new RecordFileProducer().produce(file, queue, failures, fatal);

        assertEquals(0, failures.get());
        assertNull(fatal.get());
        assertEquals(1, queue.size());
        Record record = assertInstanceOf(Record.class, queue.remove().orElseThrow());
        assertEquals(date, record.getDate());
        assertTrue(record.toString().contains("DATE: " + date + "\n"));
    }
}
