package de.ipb_halle.massbank3_export_service.api;


import de.ipb_halle.massbank3_export_service.model.Conversion;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.ipb_halle.massbank3_export_service.api.DataReader.*;


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
    public ResponseEntity<org.springframework.core.io.Resource> convertPost(Conversion conversion) {
        System.out.println("Conversion task received: " + conversion);
        String responseBody = switch (conversion.getFormat().getValue()) {
            case "nist_msp" -> conversion.getRecordList().stream()
                .map(recordToNISTMSP::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
            case "riken_msp" -> conversion.getRecordList().stream()
                .map(recordToRIKENMSP::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
            default -> conversion.getRecordList().stream()
                .map(recordToRecordString::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        };

        ByteArrayResource resource = new ByteArrayResource(responseBody.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted_records.txt");

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(resource.contentLength())
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

}
