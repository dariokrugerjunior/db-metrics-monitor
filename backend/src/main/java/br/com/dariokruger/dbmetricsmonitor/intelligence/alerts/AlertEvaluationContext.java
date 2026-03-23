package br.com.dariokruger.dbmetricsmonitor.intelligence.alerts;

import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;

public record AlertEvaluationContext(
        DatabaseHealthSnapshot snapshot,
        List<AnomalyItemResponse> anomalies
) {
}
