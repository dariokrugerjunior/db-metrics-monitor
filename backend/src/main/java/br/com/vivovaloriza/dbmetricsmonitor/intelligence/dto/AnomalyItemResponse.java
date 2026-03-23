package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalyCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalySeverity;

public record AnomalyItemResponse(
        AnomalyCode code,
        AnomalySeverity severity,
        String message,
        MetricBaselineResponse baseline
) {
}
