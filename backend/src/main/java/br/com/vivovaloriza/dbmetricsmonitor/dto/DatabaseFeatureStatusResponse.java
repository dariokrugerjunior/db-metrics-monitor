package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record DatabaseFeatureStatusResponse(
        boolean available,
        String message
) {
}
