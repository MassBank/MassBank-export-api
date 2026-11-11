package massbank_export_api.api;

import massbank.Record;
import massbank.db.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RawtextApiDelegateImpl implements RawtextApiDelegate {

    private final RecordService recordService;

    @Autowired
    public RawtextApiDelegateImpl(RecordService recordService) {
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
        final Optional<Record> recordOptional = recordService.findOptionalByIdAsRecord(accession);
        if (recordOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final Record record = recordOptional.get();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(record.toString());
    }

}
