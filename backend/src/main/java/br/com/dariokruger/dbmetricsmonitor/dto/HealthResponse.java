package br.com.dariokruger.dbmetricsmonitor.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String databaseStatus,
        Instant timestamp
) {
}
