package br.com.dariokruger.dbmetricsmonitor.intelligence.dto;

import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.AnomalyCode;
import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.AnomalySeverity;

public record AnomalyItemResponse(
        AnomalyCode code,
        AnomalySeverity severity,
        String message,
        MetricBaselineResponse baseline
) {
}
