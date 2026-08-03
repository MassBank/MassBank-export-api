package massbank_export_api.api;

public record DatabaseStatus(
        boolean ready,
        boolean reachable,
        Long activeRecordCount,
        String message) {
}
