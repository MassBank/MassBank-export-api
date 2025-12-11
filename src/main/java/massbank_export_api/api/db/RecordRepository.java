package massbank_export_api.api.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordRepository extends JpaRepository<DbRecord, Long> {

    DbRecord findByAccession(String accession);

    @Query("SELECT r.accession FROM DbRecord r")
    List<String> findAllAccessions();

}
