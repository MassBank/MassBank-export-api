package massbank_export_api.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import massbank.RecordParser;
import massbank.export.RecordToNIST_MSP;
import massbank.export.RecordToRIKEN_MSP;
import massbank_export_api.api.db.DbRecord;
import massbank_export_api.api.db.RecordServiceImplementation;
import massbank_export_api.model.Conversion;
import org.petitparser.context.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Primary
@Service
public class ConvertApiDelegateImpl implements ConvertApiDelegate {

    private final RecordServiceImplementation recordServiceImplementation;

    @Autowired
    public ConvertApiDelegateImpl(RecordServiceImplementation recordServiceImplementation) {
        this.recordServiceImplementation = recordServiceImplementation;
    }

    /**
     * POST /convert : Create a conversion task.
     *
     * @param conversion (required)
     * @return Conversion successfully completed. (status code 200)
     * @see ConvertApi#convertPost
     */
    @Override
    public ResponseEntity<Resource> convertPost(Conversion conversion) {
        String formatValue = conversion.getFormat() != null ? conversion.getFormat().getValue() : "";
        Resource resource = null;
        String filename = null;
        MediaType mediaType = null;

        final RecordParser recordparser = new RecordParser(new HashSet<>());

        if (conversion.getRecordList() == null || conversion.getRecordList().isEmpty()) {
            conversion.setRecordList(recordServiceImplementation.getAllAccessions());
        }

        switch (formatValue) {
            case "nist_msp":
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.msp";
                resource = new ByteArrayResource(
                        conversion.getRecordList().parallelStream()
                                .map(recordServiceImplementation::findByAccession)
                                .filter(Objects::nonNull)
                                .map(DbRecord::getContent)
                                .map(recordparser::parse)
                                .filter(Result::isSuccess)
                                .map(Result::get)
                                .map(record -> (massbank.Record) record)
                                .map(RecordToNIST_MSP::convert)
                                .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()))
                                .getBytes(StandardCharsets.UTF_8));
                break;
            case "riken_msp":
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.msp";
                resource = new ByteArrayResource(
                        conversion.getRecordList().parallelStream()
                                .map(recordServiceImplementation::findByAccession)
                                .filter(Objects::nonNull)
                                .map(DbRecord::getContent)
                                .map(recordparser::parse)
                                .filter(Result::isSuccess)
                                .map(Result::get)
                                .map(record -> (massbank.Record) record)
                                .map(RecordToRIKEN_MSP::convert)
                                .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()))
                                .getBytes(StandardCharsets.UTF_8));
                break;
            case "massbank":
                mediaType = MediaType.parseMediaType("application/zip");
                filename = "records.zip";
                try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        final ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                    conversion.getRecordList().parallelStream()
                            .map(recordServiceImplementation::findByAccession)
                            .filter(Objects::nonNull)
                            .map(DbRecord::getContent)
                            .forEach(record -> {
                                final String accession = record.substring(record.indexOf("ACCESSION:") + 10,
                                        record.indexOf("\n", record.indexOf("ACCESSION:"))).trim();
                                try {
                                    final ZipEntry entry = new ZipEntry(accession + ".txt");
                                    synchronized (zos) {
                                        zos.putNextEntry(entry);
                                        zos.write(record.getBytes(StandardCharsets.UTF_8));
                                        zos.closeEntry();
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException("Error adding record to zip: " + accession, e);
                                }
                            });
                    zos.finish();
                    resource = new ByteArrayResource(baos.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException("Error creating zip file", e);
                }
                break;
            case "json":
                mediaType = MediaType.parseMediaType("application/jsonl");
                try {
                    PipedOutputStream pos = new PipedOutputStream();
                    PipedInputStream pis = new PipedInputStream(pos);
                    new Thread(() -> {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            boolean first = true;
                            for (String accession : conversion.getRecordList()) {
                                DbRecord dbRecord = recordServiceImplementation.findByAccession(accession);
                                if (dbRecord == null) continue;
                                Result parseResult = recordparser.parse(dbRecord.getContent());
                                if (!parseResult.isSuccess()) continue;
                                massbank.Record record = (massbank.Record) parseResult.get();
                                String json = mapper.writeValueAsString(massbank.export.RecordToJson.convert(record));
                                if (!first) {
                                    pos.write('\n');
                                } else {
                                    first = false;
                                }
                                pos.write(json.getBytes(StandardCharsets.UTF_8));
                            }
                            pos.close();
                        } catch (IOException e) {
                            throw new RuntimeException("Error streaming JSONL", e);
                        }
                    }).start();
                    resource = new InputStreamResource(pis);
                } catch (IOException e) {
                    throw new RuntimeException("Error creating stream for JSONL", e);
                }
                filename = null;
                break;
            default:
                String message = "Missing or unsupported format value.";
                resource = new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8));
                return ResponseEntity.badRequest()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(resource);
        }

        if ("json".equals(formatValue)) {
            // Kein Download, sondern Stream
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            long contentLength = -1;
            try {
                contentLength = resource.contentLength();
            } catch (IOException ignored) {}
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(contentLength)
                    .contentType(mediaType)
                    .body(resource);
        }
    }

}
