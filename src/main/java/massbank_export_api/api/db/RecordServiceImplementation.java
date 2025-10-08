package massbank_export_api.api.db;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecordServiceImplementation implements RecordService {

    private final RecordRepository recordRepository;

    @Autowired
    public RecordServiceImplementation(final RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public List<DbRecord> getAll() {
        try {
            return recordRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all records", e);
        }
    }

    public DbRecord findByAccession(String accession) {
        try {
            return recordRepository.findByAccession(accession);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find record by accession: " + accession, e);
        }
    }

    public DbRecord insert(DbRecord record) {
        try {
            return recordRepository.saveAndFlush(record);
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert record: " + record.getAccession(), e);
        }
    }

    public void deleteAll() {
        try {
            recordRepository.deleteAll();
            recordRepository.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all records", e);
        }
    }

    public long count() {
        try {
            return recordRepository.count();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count records", e);
        }
    }

    public List<String> getAllAccessions() {
        try {
            return recordRepository.findAllAccessions();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all accessions", e);
        }
    }

}
