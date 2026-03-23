package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Instant;

public record HistoricalIncidentResponse(
        long id,
        String incidentType,
        String severity,
        String title,
        String details,
        Double metricValue,
        String metricUnit,
        String source,
        String referenceName,
        Instant createdAt
) {
}
