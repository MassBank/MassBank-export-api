package massbank_export_api.api;

import massbank.RecordParser;
import org.petitparser.context.Result;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Primary
@Service
public class ValidateApiDelegateImpl implements ValidateApiDelegate {

    final RecordParser recordparser = new RecordParser(new HashSet<>());

    @Override
    public ResponseEntity<Void> validatePost(String body) {
        Result result = recordparser.parse(body);
        if (result.isSuccess()) return new ResponseEntity<>(HttpStatus.OK);
        else {
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

}
