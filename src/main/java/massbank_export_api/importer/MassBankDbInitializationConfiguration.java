package massbank_export_api.importer;

import massbank.db.MassBankDbAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import massbank.Record;
import massbank.db.AccessionClaim;

@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(MassBankDbAutoConfiguration.class)
@EntityScan(basePackageClasses = {Record.class, AccessionClaim.class, ImportMetadata.class})
public class MassBankDbInitializationConfiguration {
}

