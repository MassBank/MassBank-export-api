package massbank_export_api.api;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.List;

import static massbank_export_api.api.DataReader.*;


@Service
public class MetadataApiDelegateImpl implements MetadataApiDelegate {
    /**
     * GET /metadata/{accession} : Get metadata for a given accession.
     *
     * @param accession  (required)
     * @return Metadata for the given accession. (status code 200)
     * @see MetadataApi#metadataAccessionGet
     */
    @Override
    public ResponseEntity<List<Object>> metadataAccessionGet(String accession) {
        var record = recordMap.get(accession);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        JsonArray metadataArray = record.createStructuredDataJsonArray();
        Type listType = new TypeToken<List<Object>>() {}.getType();
        List<Object> metadataList = new Gson().fromJson(metadataArray, listType);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(metadataList);
    }

}
