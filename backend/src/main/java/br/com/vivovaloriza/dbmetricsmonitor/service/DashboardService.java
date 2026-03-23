package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.dto.ConnectionSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DashboardSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.QueryStatsResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.RunningQueryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.SystemMetricsResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final HealthService healthService;
    private final DatabaseMonitoringService databaseMonitoringService;
    private final SystemMetricsService systemMetricsService;

    public DashboardSummaryResponse summary() {
        var health = healthService.health();
        ConnectionSummaryResponse connections = databaseMonitoringService.getConnectionSummary();
        List<RunningQueryResponse> runningQueries = databaseMonitoringService.getRunningQueries(databaseMonitoringService.defaultRunningQueryThreshold());
        QueryStatsResponse topQueries = databaseMonitoringService.getTopQueries(null);
        SystemMetricsResponse systemMetrics = systemMetricsService.collect();

        DashboardSummaryResponse.DatabaseSummary database = new DashboardSummaryResponse.DatabaseSummary(
                health.databaseStatus(),
                connections,
                new DashboardSummaryResponse.LocksSummary(
                        databaseMonitoringService.getLocks().size(),
                        databaseMonitoringService.getBlockedLocks().size(),
                        databaseMonitoringService.getBlockingLocks().size()
                ),
                new DashboardSummaryResponse.RunningQueriesSummary(runningQueries.size(), runningQueries),
                topQueries.queries(),
                databaseMonitoringService.getDatabaseSettings()
        );

        DashboardSummaryResponse.ApplicationSummary application = new DashboardSummaryResponse.ApplicationSummary(
                systemMetrics.cpu(),
                systemMetrics.memory(),
                systemMetrics.threads(),
                systemMetrics.uptime().toSeconds()
        );

        return new DashboardSummaryResponse(database, application, Instant.now());
    }
}
