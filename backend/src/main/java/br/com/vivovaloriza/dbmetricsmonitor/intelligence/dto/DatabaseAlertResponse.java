package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import java.time.Instant;
import java.util.List;

public record DatabaseAlertResponse(
        Instant generatedAt,
        int totalActiveAlerts,
        List<AlertItemResponse> alerts
) {
}
