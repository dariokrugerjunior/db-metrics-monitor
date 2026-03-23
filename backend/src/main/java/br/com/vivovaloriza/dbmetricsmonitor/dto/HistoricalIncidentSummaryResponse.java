package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record HistoricalIncidentSummaryResponse(
        int totalIncidents,
        int cpuIncidents,
        int memoryIncidents,
        int lockIncidents
) {
}
