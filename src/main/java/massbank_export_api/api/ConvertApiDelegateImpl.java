package massbank_export_api.api;

import massbank.Record;
import massbank.db.RecordService;
import massbank.export.RecordToJson;
import massbank.export.RecordToNIST_MSP;
import massbank.export.RecordToRIKEN_MSP;
import massbank_export_api.model.Conversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Primary
@Service
public class ConvertApiDelegateImpl implements ConvertApiDelegate {

    private final RecordService recordService;

    @Autowired
    public ConvertApiDelegateImpl(RecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * POST /convert : Create a conversion task.
     *
     * @param conversion (required)
     * @return Conversion successfully completed. (status code 200)
     * @see ConvertApi#convertPost
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> convertPost(Conversion conversion) {
        String formatValue = conversion.getFormat() != null ? conversion.getFormat().getValue() : "";
        final List<Record> records;
        if (conversion.getRecordList() == null || conversion.getRecordList().isEmpty()) {
            records = recordService.findAllActive();
        } else {
            Map<String, Record> activeRecordsByAccession = recordService.findAllActive().stream()
                    .collect(Collectors.toMap(Record::getAccession, Function.identity(), (first, second) -> first));
            records = conversion.getRecordList().stream()
                    .map(activeRecordsByAccession::get)
                    .filter(Objects::nonNull)
                    .toList();
        }

        Resource resource;
        String filename;
        MediaType mediaType;

        switch (formatValue) {
            case "nist_msp":
            case "riken_msp": {
                boolean isNist = formatValue.equals("nist_msp");
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.msp";
                String content = records.stream()
                        .map(isNist ? RecordToNIST_MSP::convert : RecordToRIKEN_MSP::convert)
                        .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
                resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
                break;
            }
            case "massbank": {
                mediaType = MediaType.parseMediaType("application/zip");
                filename = "records.zip";
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                    records.forEach(record -> {
                                String accession = record.getAccession();
                                try {
                                    ZipEntry entry = new ZipEntry(accession + ".txt");
                                    synchronized (zos) {
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
            }
            case "json": {
                mediaType = MediaType.APPLICATION_JSON;
                filename = "records.json";
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    String json = RecordToJson.convertRecords(records);
                    baos.write(json.getBytes(StandardCharsets.UTF_8));
                    resource = new ByteArrayResource(baos.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException("Error creating JSON file", e);
                }
                break;
            }
            default: {
                String message = "Missing or unsupported format value.";
                resource = new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8));
                mediaType = MediaType.TEXT_PLAIN;
                filename = null;
                return ResponseEntity.badRequest()
                        .contentType(mediaType)
                        .body(resource);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        if (filename != null) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        }
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
