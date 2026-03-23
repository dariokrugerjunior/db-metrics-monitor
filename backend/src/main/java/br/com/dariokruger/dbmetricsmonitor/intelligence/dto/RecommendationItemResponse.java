package br.com.dariokruger.dbmetricsmonitor.intelligence.dto;

import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.RecommendationCode;
import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.RecommendationPriority;
import java.util.List;

public record RecommendationItemResponse(
        RecommendationCode code,
        String title,
        String description,
        RecommendationPriority priority,
        String rationale,
        List<String> suggestedSteps,
        List<String> relatedSignals
) {
}
