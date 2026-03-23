package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Instant;

public record AiAnalysisHistoryResponse(
        long id,
        String dbUrlAdmin,
        String model,
        String userPrompt,
        String finalPrompt,
        String analysis,
        Instant createdAt
) {
}
