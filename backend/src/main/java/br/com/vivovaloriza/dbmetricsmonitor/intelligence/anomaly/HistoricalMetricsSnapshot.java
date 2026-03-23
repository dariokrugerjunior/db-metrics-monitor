package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import java.time.Instant;

public record HistoricalMetricsSnapshot(
        Instant collectedAt,
        int totalConnections,
        int activeConnections,
        int idleInTransactionConnections,
        int blockedLocks,
        int runningQueries,
        int longRunningQueries,
        double cacheHitPercent,
        double cpuPercent,
        double memoryPercent
) {
}
