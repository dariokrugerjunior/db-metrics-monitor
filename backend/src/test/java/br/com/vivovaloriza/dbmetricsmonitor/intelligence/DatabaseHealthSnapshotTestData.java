package br.com.vivovaloriza.dbmetricsmonitor.intelligence;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DatabaseHealthSnapshotTestData {

    private DatabaseHealthSnapshotTestData() {
    }

    public static DatabaseHealthSnapshot snapshot(
            int blockedLocks,
            int blockingLocks,
            int idleInTransaction,
            BigDecimal connectionUsagePercent,
            long maxRunningQuerySeconds,
            BigDecimal cacheHitPercent,
            double cpuPercent,
            double memoryPercent,
            int totalIncidents,
            int lockIncidents
    ) {
        return new DatabaseHealthSnapshot(
                Instant.parse("2026-03-23T12:00:00Z"),
                "test",
                "UP",
                new DatabaseHealthSnapshot.ConnectionMetricsSnapshot(80, 20, 60, idleInTransaction, 100, connectionUsagePercent),
                new DatabaseHealthSnapshot.LockMetricsSnapshot(12, blockedLocks, blockingLocks),
                new DatabaseHealthSnapshot.QueryMetricsSnapshot(4, maxRunningQuerySeconds >= 10 ? 1 : 0, maxRunningQuerySeconds),
                List.of(new DatabaseHealthSnapshot.TopQuerySnapshot("select 1", 10L, BigDecimal.valueOf(15000), BigDecimal.valueOf(1500))),
                new DatabaseHealthSnapshot.CacheMetricsSnapshot(cacheHitPercent, "OPTIMAL"),
                new DatabaseHealthSnapshot.ResourceMetricsSnapshot(cpuPercent),
                new DatabaseHealthSnapshot.ResourceMetricsSnapshot(memoryPercent),
                new DatabaseHealthSnapshot.HistoricalIncidentSnapshot(totalIncidents, 0, 0, lockIncidents),
                new DatabaseHealthSnapshot.DerivedSignalsSnapshot(
                        BigDecimal.valueOf(15000),
                        BigDecimal.valueOf(30000),
                        75.0d,
                        25.0d
                )
        );
    }
}
