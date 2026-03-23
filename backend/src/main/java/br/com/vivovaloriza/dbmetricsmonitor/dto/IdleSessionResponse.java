package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Duration;
import java.time.OffsetDateTime;

public record IdleSessionResponse(
        Long pid,
        String userName,
        String database,
        Duration idleInTransactionDuration,
        OffsetDateTime xactStart,
        String query,
        String applicationName,
        String clientAddr
) {
}
