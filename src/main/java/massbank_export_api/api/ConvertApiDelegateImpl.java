package massbank_export_api.api;


import massbank_export_api.model.Conversion;
import massbank.export.RecordToNIST_MSP;
import massbank.export.RecordToRIKEN_MSP;
import massbank.Record;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static massbank_export_api.api.DataReader.*;


@Primary
@Service
public class ConvertApiDelegateImpl implements ConvertApiDelegate {
    /**
     * POST /convert : Create a conversion task.
     *
     * @param conversion  (required)
     * @return Conversion successfully completed. (status code 200)
     * @see ConvertApi#convertPost
     */
    @Override
    public ResponseEntity<Resource> convertPost(Conversion conversion) {
        final ByteArrayResource resource;
        final String filename;
        final MediaType mediaType;

        switch (Objects.requireNonNull(conversion.getFormat()).getValue()) {
            case "nist_msp":
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.msp";
                resource = new ByteArrayResource(
                        conversion.getRecordList().stream()
                                .map(recordMap::get)
                                .filter(Objects::nonNull)
                                .map(RecordToNIST_MSP::convert)
                                .collect(Collectors.joining())
                                .getBytes(StandardCharsets.UTF_8)
                );
                break;
            case "riken_msp":
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.msp";
                resource = new ByteArrayResource(
                        conversion.getRecordList().stream()
                                .map(recordMap::get)
                                .filter(Objects::nonNull)
                                .map(RecordToRIKEN_MSP::convert)
                                .collect(Collectors.joining())
                                .getBytes(StandardCharsets.UTF_8)
                );
                break;
            case "massbank":
                mediaType = MediaType.parseMediaType("application/zip");
                filename = "records.zip";
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                    conversion.getRecordList().stream()
                            .map(recordMap::get)
                            .filter(Objects::nonNull)
                            .forEach(record -> {
                                try {
                                    ZipEntry entry = new ZipEntry(record.ACCESSION()+".txt");
                                    zos.putNextEntry(entry);
                                    zos.write(record.toString().getBytes(StandardCharsets.UTF_8));
                                    zos.closeEntry();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                    zos.finish();
                    resource = new ByteArrayResource(baos.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                mediaType = MediaType.TEXT_PLAIN;
                filename = "records.txt";
                resource = new ByteArrayResource(
                        conversion.getRecordList().stream()
                                .map(recordMap::get)
                                .filter(Objects::nonNull)
                                .map(Record::toString)
                                .collect(Collectors.joining("\n"))
                                .getBytes(StandardCharsets.UTF_8)
                );
        };


        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(resource.contentLength())
            .contentType(mediaType)
            .body(resource);
    }

}
