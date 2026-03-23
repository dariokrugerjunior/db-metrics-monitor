package br.com.dariokruger.dbmetricsmonitor.intelligence.recommendation;

import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;

public record RecommendationEvaluationContext(
        DatabaseHealthSnapshot snapshot,
        List<AlertItemResponse> alerts,
        List<AnomalyItemResponse> anomalies
) {
}
