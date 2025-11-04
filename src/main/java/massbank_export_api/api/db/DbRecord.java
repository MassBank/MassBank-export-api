package massbank_export_api.api.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "record",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_record_accession", columnNames = { "accession" })
    })
public class DbRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "accession", columnDefinition = "VARCHAR(120)", nullable = false)
    private String accession;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

}