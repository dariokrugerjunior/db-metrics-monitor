package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

public record MetricBaselineResponse(
        String metric,
        double currentValue,
        double average,
        double median,
        double min,
        double max,
        double percentDeviation
) {
}
