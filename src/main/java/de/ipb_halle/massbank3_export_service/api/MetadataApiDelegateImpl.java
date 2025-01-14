package de.ipb_halle.massbank3_export_service.api;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.ipb_halle.massbank3_export_service.api.DataReader.*;


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
        JsonArray metadataArray = recordToMetadata.get(accession);

        if (metadataArray == null) {
            return ResponseEntity.notFound().build();
        }

        List<Object> metadataList = new Gson().fromJson(metadataArray, List.class);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(metadataList);
    }

}
