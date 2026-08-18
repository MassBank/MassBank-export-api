package massbank_export_api.importer;

import massbank_export_api.api.DataLoadResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "massbank_export_api")
public class DataImportApplication {

    private static final Logger logger = LogManager.getLogger(DataImportApplication.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DataImportApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(application.run(args)));
    }

    @Bean
    public CommandLineRunner importMassBankData(DataReader dataReader) {
        return args -> {
            DataLoadResult result = dataReader.readData();
            if (!result.successful()) {
                throw new IllegalStateException("MassBank data import failed: " + result.message());
            }
            logger.info("MassBank data import finished: {}", result);
        };
    }
}
