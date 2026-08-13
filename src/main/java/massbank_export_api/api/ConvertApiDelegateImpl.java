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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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

        if (formatValue.isBlank()) {
            String message = "Missing or unsupported format value.";
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8)));
        }

        final List<Record> records;
        if (conversion.getRecordList() == null || conversion.getRecordList().isEmpty()) {
            records = recordService.findAllActive();
        } else {
            records = conversion.getRecordList().stream()
                    .map(recordService::findOptionalByIdAsRecord)
                    .flatMap(Optional::stream)
                    .toList();
        }

        Resource responseBody;
        String filename;
        MediaType mediaType;

        switch (formatValue) {
            case "nist_msp":
            case "riken_msp": {
                boolean isNist = formatValue.equals("nist_msp");
                byte[] bytes;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
                    boolean first = true;
                    for (Record record : records) {
                        if (!first) {
                            writer.write(System.lineSeparator());
                        }
                        String content = isNist ? RecordToNIST_MSP.convert(record) : RecordToRIKEN_MSP.convert(record);
                        writer.write(content);
                        first = false;
                    }
                    writer.write(System.lineSeparator());
                    writer.flush();
                    bytes = baos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Error creating MSP content", e);
                }
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.msp";
                responseBody = new ByteArrayResource(bytes);
                break;
            }
            case "massbank": {
                byte[] bytes;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                    for (Record record : records) {
                        ZipEntry entry = new ZipEntry(record.getAccession() + ".txt");
                        zos.putNextEntry(entry);
                        zos.write(record.toString().getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();
                    }
                    zos.finish();
                    bytes = baos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Error creating ZIP content", e);
                }
                mediaType = MediaType.parseMediaType("application/zip");
                filename = "records.zip";
                responseBody = new ByteArrayResource(bytes);
                break;
            }
            case "json": {
                String json = RecordToJson.convertRecords(records);
                mediaType = MediaType.APPLICATION_JSON;
                filename = "records.json";
                responseBody = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
                break;
            }
            default: {
                String message = "Missing or unsupported format value.";
                return ResponseEntity.badRequest()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(new ByteArrayResource(message.getBytes(StandardCharsets.UTF_8)));
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(responseBody);
    }


}
