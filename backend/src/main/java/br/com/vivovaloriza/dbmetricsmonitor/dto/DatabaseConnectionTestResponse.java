package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record DatabaseConnectionTestResponse(
        boolean success,
        String databaseProductName,
        String databaseVersion,
        String currentDatabase,
        long responseTimeMs,
        String message
) {
}
