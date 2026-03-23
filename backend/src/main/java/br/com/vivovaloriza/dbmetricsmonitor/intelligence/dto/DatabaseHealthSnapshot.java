package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DatabaseHealthSnapshot(
        Instant collectedAt,
        String environment,
        String databaseStatus,
        ConnectionMetricsSnapshot connections,
        LockMetricsSnapshot locks,
        QueryMetricsSnapshot runningQueries,
        List<TopQuerySnapshot> topQueries,
        CacheMetricsSnapshot cache,
        ResourceMetricsSnapshot cpu,
        ResourceMetricsSnapshot memory,
        HistoricalIncidentSnapshot historicalIncidents,
        DerivedSignalsSnapshot derivedSignals
) {

    public record ConnectionMetricsSnapshot(
            int totalConnections,
            int activeConnections,
            int idleConnections,
            int idleInTransactionConnections,
            int maxConnections,
            BigDecimal usagePercent
    ) {
    }

    public record LockMetricsSnapshot(
            int totalLocks,
            int blockedLocks,
            int blockingLocks
    ) {
    }

    public record QueryMetricsSnapshot(
            int runningQueries,
            int longRunningQueries,
            long maxRunningQuerySeconds
    ) {
    }

    public record TopQuerySnapshot(
            String query,
            Long calls,
            BigDecimal totalExecTime,
            BigDecimal meanExecTime
    ) {
    }

    public record CacheMetricsSnapshot(
            BigDecimal cacheHitPercent,
            String classification
    ) {
    }

    public record ResourceMetricsSnapshot(
            double percent
    ) {
    }

    public record HistoricalIncidentSnapshot(
            int totalIncidentsLast24Hours,
            int cpuIncidentsLast24Hours,
            int memoryIncidentsLast24Hours,
            int lockIncidentsLast24Hours
    ) {
    }

    public record DerivedSignalsSnapshot(
            BigDecimal maxTopQueryTotalExecTime,
            BigDecimal totalTopQueryExecTime,
            double idleConnectionRatioPercent,
            double activeConnectionRatioPercent
    ) {
    }
}
