package massbank_export_api.api;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;

import static massbank_export_api.api.DataReader.recordMap;


@Service
public class RawtextApiDelegateImpl implements RawtextApiDelegate {
    /**
     * GET /rawtext/{accession} : Get rawtext for a given accession.
     *
     * @param accession  (required)
     * @return Metadata for the given accession. (status code 200)
     * @see MetadataApi#metadataAccessionGet
     */
    @Override
    public ResponseEntity<String> rawtextAccessionGet(String accession) {
        var record = recordMap.get(accession);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(record.toString());
    }

}
