package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import java.time.Instant;
import java.util.List;

public record AnomalyDetectionResponse(
        Instant generatedAt,
        boolean baselineAvailable,
        String message,
        List<AnomalyItemResponse> anomalies
) {
}
