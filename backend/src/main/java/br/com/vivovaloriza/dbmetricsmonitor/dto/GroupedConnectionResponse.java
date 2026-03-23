package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record GroupedConnectionResponse(
        String name,
        int connections
) {
}
