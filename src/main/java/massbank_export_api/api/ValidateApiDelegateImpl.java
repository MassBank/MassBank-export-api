package massbank_export_api.api;

import massbank.RecordParser;
import massbank_export_api.model.ValidationError;
import org.petitparser.context.Result;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;

@Primary
@Service
public class ValidateApiDelegateImpl implements ValidateApiDelegate {

    private final RecordParser recordParser;

    public ValidateApiDelegateImpl() {
        recordParser = new RecordParser(new HashSet<>());
    }

    @Override
    public ResponseEntity<Void> validatePost(String body) {
        if (body == null || body.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Result result = recordParser.parse(body);
        if (result.isSuccess()) {
            return ResponseEntity.ok().build();
        }

        String message = result.getMessage();
        int pos = result.getPosition();

        String[] tokens = body.split("\\n");
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

        ValidationError error = new ValidationError();
        error.setMessage(message);
        error.setLine(lineNumber);
        error.setColumn(col);

        ResponseEntity<ValidationError> respWithBody =
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(error);

        @SuppressWarnings("unchecked")
        ResponseEntity<Void> casted = (ResponseEntity<Void>)(ResponseEntity<?>) respWithBody;
        return casted;
    }

}
