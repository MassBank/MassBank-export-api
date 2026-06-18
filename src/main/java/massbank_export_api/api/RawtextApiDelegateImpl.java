package massbank_export_api.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import massbank_export_api.api.db.DbRecord;
import massbank_export_api.api.db.RecordService2;

@Service
public class RawtextApiDelegateImpl implements RawtextApiDelegate {

    private final RecordService2 recordService;

    @Autowired
    public RawtextApiDelegateImpl(RecordService2 recordService) {
        this.recordService = recordService;
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
        final DbRecord record = recordService.findByAccession(accession);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(record.getContent());
    }

}
