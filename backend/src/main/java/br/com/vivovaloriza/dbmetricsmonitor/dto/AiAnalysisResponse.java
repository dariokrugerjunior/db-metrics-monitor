package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Instant;

public record AiAnalysisResponse(
        String model,
        String prompt,
        String analysis,
        Instant generatedAt
) {
}
