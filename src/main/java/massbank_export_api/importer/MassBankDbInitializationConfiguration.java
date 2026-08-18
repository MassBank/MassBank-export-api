package massbank_export_api.importer;

import massbank.db.MassBankDbAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(MassBankDbAutoConfiguration.class)
public class MassBankDbInitializationConfiguration {
}


