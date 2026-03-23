package br.com.dariokruger.dbmetricsmonitor.dto;

public record GroupedConnectionResponse(
        String name,
        int connections
) {
}
