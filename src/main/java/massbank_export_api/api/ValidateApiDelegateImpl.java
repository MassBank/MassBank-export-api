package massbank_export_api.api;

import massbank.RecordParser;
import massbank_export_api.model.Validation;
import massbank_export_api.model.ValidationResult;

import org.petitparser.context.Result;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Primary
@Service
public class ValidateApiDelegateImpl implements ValidateApiDelegate {

    private final RecordParser recordParser;

    public ValidateApiDelegateImpl() {
        final Set<String> configKeys = new HashSet<>();
        configKeys.add("validate");
        recordParser = new RecordParser(configKeys);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<ValidationResult> validatePost(Validation validation) {
        if (validation == null || validation.getText() == null) {
            String json = "{\"message\":\"Invalid request payload.\"}";
            return (ResponseEntity<ValidationResult>) (ResponseEntity<?>) ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
        }

        final String recordText = validation.getText();

        ResponseEntity<ValidationResult> respWithBody;
        final ValidationResult validationResult = new ValidationResult();
        final Result result = recordParser.parse(recordText);
        if (result.isSuccess()) {
            validationResult.setMessage("Record is valid.");
            validationResult.setLine(null);
            validationResult.setColumn(null);

            respWithBody = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(validationResult);
        } else {
            final String message = result.getMessage();
            final int pos = result.getPosition();

            final String[] tokens = recordText.split("\\n");
            int offset = 0;
            int lineNumber = 1;
            int col = 0;
            for (String token : tokens) {
                offset += token.length() + 1;
                if (pos < offset) {
                    col = pos - (offset - (token.length() + 1));
                    break;
                }
                lineNumber++;
            }

            validationResult.setMessage(message);
            validationResult.setLine(lineNumber);
            validationResult.setColumn(col);

            respWithBody = ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(validationResult);
        }

        return respWithBody;
    }

}
