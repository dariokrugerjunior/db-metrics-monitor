package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record TableSizeResponse(
        String schemaName,
        String tableName,
        String totalSize,
        String tableSize,
        String indexesSize,
        long estimatedRows
) {
}
