package massbank_export_api.api;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import massbank.Record;
import massbank.db.RecordService;
import massbank.export.RecordToJson;
import massbank.export.RecordToNIST_MSP;
import massbank.export.RecordToRIKEN_MSP;
import massbank_export_api.model.Conversion;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class ConvertApiDelegateImpl implements ConvertApiDelegate {
    private static final int PAGE_SIZE = 128;
    private final EntityManager db;
    private final RecordService recordService;
    private final HttpServletResponse response;

    public ConvertApiDelegateImpl(EntityManager db, RecordService recordService, HttpServletResponse response) {
        this.db = db;
        this.recordService = recordService;
        this.response = response;
    }

    /**
     * Streams a MassBank export in the requested format.
     *
     * @param conversion the export format and optional list of accessions.
     *                   If the list is missing or empty, exports all active records.
     * @return an empty HTTP 200 response entity after writing the export directly
     *         to the HTTP response body, or HTTP 400 if the format is missing
     * @see ConvertApi#convertPost
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResponseEntity<Resource> convertPost(Conversion conversion) {
        if (conversion.getFormat() == null) {
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(new ByteArrayResource("{\"message\":\"Missing or unsupported format value.\"}".getBytes(UTF_8)));
        }
        String format = conversion.getFormat().getValue();
        boolean zip = format.equals("massbank");
        boolean json = format.equals("json");
        response.setContentType(zip ? "application/zip" : json ? "application/json" : "text/plain");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"records."
                + (zip ? "zip" : json ? "json" : "msp") + "\"");

        var recordList = conversion.getRecordList();
        boolean all = recordList == null || recordList.isEmpty();
        var requested = all ? List.<String>of() : recordList.stream().filter(Objects::nonNull).distinct().toList();
        try {
            var out = response.getOutputStream();
            try (var archive = zip ? new ZipOutputStream(out, UTF_8) : null) {
                if (json) out.write('[');
                String after = "";
                int offset = 0;
                boolean first = true;
                while (all || offset < requested.size()) {
                    List<Record> page;
                    if (all) {
                        page = db.createQuery("select r from Record r where r.accession > :after order by r.accession", Record.class)
                                .setParameter("after", after).setMaxResults(PAGE_SIZE).getResultList();
                        if (page.isEmpty()) break;
                        after = page.getLast().getAccession();
                    } else {
                        int end = Math.min(offset + PAGE_SIZE, requested.size());
                        var ids = requested.subList(offset, end);
                        var found = recordService.findAllActiveById(ids).stream()
                                .collect(Collectors.toMap(Record::getAccession, Function.identity()));
                        page = ids.stream().map(found::get).filter(Objects::nonNull).toList();
                        offset = end;
                    }
                    for (Record record : page) {
                        if (zip) {
                            archive.putNextEntry(new ZipEntry(record.getAccession() + ".txt"));
                            archive.write(record.toString().getBytes(UTF_8));
                            archive.closeEntry();
                        } else {
                            if (!first) out.write(json ? ',' : '\n');
                            String text = json ? RecordToJson.convert(record)
                                    : format.equals("nist_msp") ? RecordToNIST_MSP.convert(record)
                                    : RecordToRIKEN_MSP.convert(record);
                            out.write(text.getBytes(UTF_8));
                        }
                        first = false;
                    }
                    db.clear();
                }
                if (!zip) out.write(json ? ']' : '\n');
                if (zip) archive.finish();
                out.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.ok().build();
    }
}
