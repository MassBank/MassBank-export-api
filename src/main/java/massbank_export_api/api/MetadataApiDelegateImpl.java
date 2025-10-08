package massbank_export_api.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import org.petitparser.context.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.google.gson.reflect.TypeToken;

import massbank.RecordParser;
import massbank_export_api.api.db.DbRecord;
import massbank_export_api.api.db.RecordServiceImplementation;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;

@Service
public class MetadataApiDelegateImpl implements MetadataApiDelegate {

    private final RecordServiceImplementation recordServiceImplementation;

    @Autowired
    public MetadataApiDelegateImpl(RecordServiceImplementation recordServiceImplementation) {
        this.recordServiceImplementation = recordServiceImplementation;
    }

    /**
     * GET /metadata/{accession} : Get metadata for a given accession.
     *
     * @param accession (required)
     * @return Metadata for the given accession. (status code 200)
     * @see MetadataApi#metadataAccessionGet
     */
    @Override
    public ResponseEntity<List<Object>> metadataAccessionGet(String accession) {
        final DbRecord record = recordServiceImplementation.findByAccession(accession);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        final RecordParser recordparser = new RecordParser(new HashSet<>());
        final Result parsedResult = recordparser.parse(record.getContent());
        if (!parsedResult.isSuccess()) {
            return ResponseEntity.badRequest().build();
        }
        final massbank.Record recordObj = parsedResult.get();
        final JsonArray metadataArray = recordObj.createStructuredDataJsonArray();

        Type listType = new TypeToken<List<Object>>() {
        }.getType();
        List<Object> metadataList = new Gson().fromJson(metadataArray, listType);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(metadataList);
    }

}
