package massbank_export_api.api;

import massbank.RecordParser;
import massbank_export_api.model.Validation;
import massbank_export_api.model.ValidationError;
import org.petitparser.context.Result;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Primary
@Service
public class ValidateApiDelegateImpl implements ValidateApiDelegate {

    private final RecordParser recordParser;

    public ValidateApiDelegateImpl() {
        recordParser = new RecordParser(new HashSet<>());
    }

    @Override
    public ResponseEntity<Void> validatePost(Validation validation) {
        if (validation == null || validation.getText() == null) {
            return ResponseEntity.badRequest().build();
        }

        final String recordText = validation.getText();

        ResponseEntity<ValidationError> respWithBody;
        final ValidationError error = new ValidationError();
        final Result result = recordParser.parse(recordText);
        if (result.isSuccess()) {
            error.setMessage("Record is valid.");
            error.setLine(null);
            error.setColumn(null);

            respWithBody = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(error);
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

            error.setMessage(message);
            error.setLine(lineNumber);
            error.setColumn(col);

            respWithBody = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
        }

        @SuppressWarnings("unchecked")
        final ResponseEntity<Void> casted = (ResponseEntity<Void>) (ResponseEntity<?>) respWithBody;

        return casted;
    }

}
