package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.util.List;

public record HistoricalIncidentPageResponse(
        List<HistoricalIncidentResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
