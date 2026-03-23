package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import java.time.Instant;
import java.util.List;

public record DatabaseIntelligenceOverviewResponse(
        DatabaseHealthScoreResponse score,
        List<AlertItemResponse> alerts,
        List<AnomalyItemResponse> anomalies,
        List<RecommendationItemResponse> recommendations,
        Instant generatedAt,
        String anomalyMessage
) {
}
