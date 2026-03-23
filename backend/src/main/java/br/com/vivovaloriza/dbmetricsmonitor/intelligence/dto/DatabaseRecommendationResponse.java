package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import java.time.Instant;
import java.util.List;

public record DatabaseRecommendationResponse(
        Instant generatedAt,
        List<RecommendationItemResponse> recommendations
) {
}
