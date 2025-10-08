package massbank_export_api.api.db;

import java.util.List;

public interface RecordService {

    public List<DbRecord> getAll();

    public DbRecord findByAccession(String accession);

    public DbRecord insert(DbRecord record);

    public void deleteAll();

    public long count();

    public List<String> getAllAccessions();

}
