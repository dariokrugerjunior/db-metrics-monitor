package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;

public record RecommendationEvaluationContext(
        DatabaseHealthSnapshot snapshot,
        List<AlertItemResponse> alerts,
        List<AnomalyItemResponse> anomalies
) {
}
