package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;

public record AlertEvaluationContext(
        DatabaseHealthSnapshot snapshot,
        List<AnomalyItemResponse> anomalies
) {
}
