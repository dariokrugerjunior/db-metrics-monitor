package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Duration;
import java.time.OffsetDateTime;

public record LockInfoResponse(
        Long pid,
        String userName,
        String database,
        String state,
        String lockType,
        String relation,
        String query,
        OffsetDateTime queryStart,
        OffsetDateTime xactStart,
        Duration queryDuration,
        Duration transactionDuration,
        Long blockedByPid,
        String blockedByQuery,
        String applicationName,
        String clientAddr,
        String waitEventType,
        String waitEvent
) {
}
