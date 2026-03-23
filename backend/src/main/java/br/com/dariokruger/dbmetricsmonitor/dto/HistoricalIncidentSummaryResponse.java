package br.com.dariokruger.dbmetricsmonitor.dto;

public record HistoricalIncidentSummaryResponse(
        int totalIncidents,
        int cpuIncidents,
        int memoryIncidents,
        int lockIncidents
) {
}
