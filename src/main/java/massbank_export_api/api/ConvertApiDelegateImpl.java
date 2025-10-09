package massbank_export_api.api;

import massbank_export_api.api.db.DbRecord;
import massbank_export_api.api.db.RecordServiceImplementation;
import massbank_export_api.model.Conversion;
import massbank.Record;
import massbank.RecordParser;
import massbank.export.RecordToNIST_MSP;
import massbank.export.RecordToRIKEN_MSP;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.petitparser.context.Result;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        final ByteArrayResource resource;
        final String filename;
        final MediaType mediaType;

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
                                .collect(Collectors.joining())
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
                                .collect(Collectors.joining())
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
                                    synchronized (zos) { // Synchronize access to ZipOutputStream
                                        zos.putNextEntry(entry);
                                        zos.write(record.toString().getBytes(StandardCharsets.UTF_8));
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
            default:
                String message = "Missing or unsupported format value.";
                resource = new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8));
                return ResponseEntity.badRequest()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(resource);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .contentType(mediaType)
                .body(resource);
    }

}
