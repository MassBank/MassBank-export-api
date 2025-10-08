package massbank_export_api.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import massbank_export_api.api.db.DbRecord;
import massbank_export_api.api.db.RecordServiceImplementation;

@Service
public class RawtextApiDelegateImpl implements RawtextApiDelegate {

    private final RecordServiceImplementation recordServiceImplementation;

    @Autowired
    public RawtextApiDelegateImpl(RecordServiceImplementation recordServiceImplementation) {
        this.recordServiceImplementation = recordServiceImplementation;
    }

    /**
     * GET /rawtext/{accession} : Get rawtext for a given accession.
     *
     * @param accession (required)
     * @return Metadata for the given accession. (status code 200)
     * @see MetadataApi#metadataAccessionGet
     */
    @Override
    public ResponseEntity<String> rawtextAccessionGet(String accession) {
        final DbRecord record = recordServiceImplementation.findByAccession(accession);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(record.getContent());
    }

}
