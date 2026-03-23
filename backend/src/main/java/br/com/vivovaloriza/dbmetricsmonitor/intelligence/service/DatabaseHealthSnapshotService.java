package br.com.vivovaloriza.dbmetricsmonitor.intelligence.service;

import br.com.vivovaloriza.dbmetricsmonitor.dto.CacheHitRatioResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.ConnectionSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.QueryStatsResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.RunningQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.SystemMetricsResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.service.DatabaseMonitoringService;
import br.com.vivovaloriza.dbmetricsmonitor.service.HealthService;
import br.com.vivovaloriza.dbmetricsmonitor.service.HistoricalIncidentService;
import br.com.vivovaloriza.dbmetricsmonitor.service.SystemMetricsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthSnapshotService {

    private final HealthService healthService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final SystemMetricsService systemMetricsService;
    private final HistoricalIncidentService historicalIncidentService;
    private final DatabaseIntelligenceProperties properties;

    public DatabaseHealthSnapshotService(
            HealthService healthService,
            DatabaseMonitoringService databaseMonitoringService,
            SystemMetricsService systemMetricsService,
            HistoricalIncidentService historicalIncidentService,
            DatabaseIntelligenceProperties properties
    ) {
        this.healthService = healthService;
        this.databaseMonitoringService = databaseMonitoringService;
        this.systemMetricsService = systemMetricsService;
        this.historicalIncidentService = historicalIncidentService;
        this.properties = properties;
    }

    public DatabaseHealthSnapshot capture() {
        Instant collectedAt = Instant.now();
        var health = healthService.health();
        ConnectionSummaryResponse connections = databaseMonitoringService.getConnectionSummary();
        var allLocks = databaseMonitoringService.getLocks();
        var blockedLocks = databaseMonitoringService.getBlockedLocks();
        var blockingLocks = databaseMonitoringService.getBlockingLocks();
        List<RunningQueryResponse> runningQueries = databaseMonitoringService.getRunningQueries(0);
        QueryStatsResponse topQueries = databaseMonitoringService.getSlowQueries(null);
        CacheHitRatioResponse cache = databaseMonitoringService.getCacheHitRatio();
        SystemMetricsResponse systemMetrics = systemMetricsService.collect();
        HistoricalIncidentSummaryResponse incidents = historicalIncidentService.getSummaryLast24Hours();

        long maxRunningQuerySeconds = runningQueries.stream()
                .map(RunningQueryResponse::duration)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(Duration::toSeconds)
                .orElse(0L);
        int longRunningQueries = (int) runningQueries.stream()
                .filter(query -> query.duration() != null && query.duration().toSeconds() >= 10)
                .count();
        double memoryPercent = systemMetrics.memory().jvmTotalMemoryBytes() > 0
                ? round((systemMetrics.memory().heapUsedBytes() * 100.0d) / systemMetrics.memory().jvmTotalMemoryBytes())
                : 0.0d;

        List<DatabaseHealthSnapshot.TopQuerySnapshot> topQuerySnapshots = topQueries.queries().stream()
                .map(query -> new DatabaseHealthSnapshot.TopQuerySnapshot(
                        query.query(),
                        query.calls(),
                        query.totalExecTime(),
                        query.meanExecTime()
                ))
                .toList();

        BigDecimal totalTopQueryExecTime = topQuerySnapshots.stream()
                .map(DatabaseHealthSnapshot.TopQuerySnapshot::totalExecTime)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxTopQueryExecTime = topQuerySnapshots.stream()
                .map(DatabaseHealthSnapshot.TopQuerySnapshot::totalExecTime)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        return new DatabaseHealthSnapshot(
                collectedAt,
                properties.getEnvironment(),
                health.databaseStatus(),
                new DatabaseHealthSnapshot.ConnectionMetricsSnapshot(
                        connections.totalConnections(),
                        connections.activeConnections(),
                        connections.idleConnections(),
                        connections.idleInTransactionConnections(),
                        connections.maxConnections(),
                        connections.usagePercent()
                ),
                new DatabaseHealthSnapshot.LockMetricsSnapshot(
                        allLocks.size(),
                        blockedLocks.size(),
                        blockingLocks.size()
                ),
                new DatabaseHealthSnapshot.QueryMetricsSnapshot(
                        runningQueries.size(),
                        longRunningQueries,
                        maxRunningQuerySeconds
                ),
                topQuerySnapshots,
                new DatabaseHealthSnapshot.CacheMetricsSnapshot(
                        cache.cacheHitRatioPercent(),
                        classifyCache(cache.cacheHitRatioPercent().doubleValue())
                ),
                new DatabaseHealthSnapshot.ResourceMetricsSnapshot(round(systemMetrics.cpu().processCpuUsage())),
                new DatabaseHealthSnapshot.ResourceMetricsSnapshot(memoryPercent),
                new DatabaseHealthSnapshot.HistoricalIncidentSnapshot(
                        incidents.totalIncidents(),
                        incidents.cpuIncidents(),
                        incidents.memoryIncidents(),
                        incidents.lockIncidents()
                ),
                new DatabaseHealthSnapshot.DerivedSignalsSnapshot(
                        maxTopQueryExecTime,
                        totalTopQueryExecTime,
                        ratioPercent(connections.idleConnections(), connections.totalConnections()),
                        ratioPercent(connections.activeConnections(), connections.totalConnections())
                )
        );
    }

    private double ratioPercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0d;
        }
        return round((numerator * 100.0d) / denominator);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String classifyCache(double cacheHitPercent) {
        if (cacheHitPercent >= 99.0d) {
            return "OPTIMAL";
        }
        if (cacheHitPercent >= 97.0d) {
            return "ACCEPTABLE";
        }
        return "LOW";
    }
}
