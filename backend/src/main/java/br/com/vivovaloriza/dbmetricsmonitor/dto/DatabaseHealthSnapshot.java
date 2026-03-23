package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DatabaseHealthSnapshot(
        ConnectionsSnapshot connections,
        LocksSnapshot locks,
        QueriesSnapshot queries,
        CacheSnapshot cache,
        SystemSnapshot system,
        List<String> alerts,
        Instant timestamp
) {

    public record ConnectionsSnapshot(
            int totalConnections,
            int activeConnections,
            int idleConnections,
            int idleInTransactionConnections,
            int maxConnections,
            BigDecimal usagePercent,
            List<ConnectionGroupSnapshot> byUser,
            List<ConnectionGroupSnapshot> byApplication
    ) {
    }

    public record ConnectionGroupSnapshot(
            String name,
            int totalConnections
    ) {
    }

    public record LocksSnapshot(
            int totalLocks,
            int blockedLocks,
            int blockingLocks,
            List<LockDetailSnapshot> blockedSessions
    ) {
    }

    public record LockDetailSnapshot(
            Long pid,
            String relation,
            String state,
            String applicationName,
            String waitEventType,
            String waitEvent,
            String duration,
            Long blockedByPid,
            String query
    ) {
    }

    public record QueriesSnapshot(
            int runningQueries,
            List<RunningQuerySnapshot> runningSessions,
            List<QueryDetailSnapshot> topQueries,
            List<SettingSnapshot> settings
    ) {
    }

    public record RunningQuerySnapshot(
            Long pid,
            String duration,
            String state,
            String waitEvent,
            String query
    ) {
    }

    public record QueryDetailSnapshot(
            BigDecimal meanExecTime,
            BigDecimal totalExecTime,
            Long calls,
            String query
    ) {
    }

    public record SettingSnapshot(
            String name,
            String value
    ) {
    }

    public record CacheSnapshot(
            BigDecimal cacheHitPercent,
            String classification,
            long heapBlksRead,
            long heapBlksHit
    ) {
    }

    public record SystemSnapshot(
            String databaseStatus,
            double cpuPercent,
            double memoryPercent,
            int liveThreads,
            long uptimeSeconds
    ) {
    }
}
