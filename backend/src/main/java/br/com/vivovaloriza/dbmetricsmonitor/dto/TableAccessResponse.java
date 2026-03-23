package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record TableAccessResponse(
        String schemaName,
        String tableName,
        long seqScan,
        long idxScan,
        long totalReads,
        long totalWrites,
        long liveTuples
) {
}
