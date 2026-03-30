package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record OpenAiConnectionTestResponse(
        boolean success,
        int modelCount,
        long responseTimeMs,
        String message
) {
}
