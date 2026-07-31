package massbank_export_api.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import massbank.Record;
import massbank.db.RecordService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

@Service
public class MetadataApiDelegateImpl implements MetadataApiDelegate {

    private final RecordService recordService;

    @Autowired
    public MetadataApiDelegateImpl(RecordService recordService) {
        this.recordService = recordService;
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
        final Record record = recordService.findByIdAsRecord(accession);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        final JsonArray metadataArray = record.createStructuredDataJsonArray();

        Type listType = new TypeToken<List<Object>>() {
        }.getType();
        List<Object> metadataList = new Gson().fromJson(metadataArray, listType);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(metadataList);
    }

}
