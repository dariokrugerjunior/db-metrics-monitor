package br.com.dariokruger.dbmetricsmonitor.dto;

public record OpenAiConnectionTestResponse(
        boolean success,
        int modelCount,
        long responseTimeMs,
        String message
) {
}
