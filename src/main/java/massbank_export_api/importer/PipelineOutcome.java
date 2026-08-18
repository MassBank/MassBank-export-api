package massbank_export_api.importer;

public record PipelineOutcome(int savedRecords, int failedRecords, Throwable fatalError) {

    public boolean hasFatalError() {
        return fatalError != null;
    }
}


