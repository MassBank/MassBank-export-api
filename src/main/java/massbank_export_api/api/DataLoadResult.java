package massbank_export_api.api;

public record DataLoadResult(
        int totalFiles,
        int savedRecords,
        int failedRecords,
        long activeRecordCount,
        boolean successful,
        String message) {
}
