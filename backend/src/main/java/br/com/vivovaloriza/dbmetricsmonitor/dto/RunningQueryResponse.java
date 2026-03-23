package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Duration;

public record RunningQueryResponse(
        Long pid,
        String userName,
        String database,
        Duration duration,
        String state,
        String waitEvent,
        String query
) {
}
