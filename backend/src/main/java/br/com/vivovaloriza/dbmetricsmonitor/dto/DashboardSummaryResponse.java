package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Instant;
import java.util.List;

public record DashboardSummaryResponse(
        DatabaseSummary database,
        ApplicationSummary application,
        Instant generatedAt
) {
    public record DatabaseSummary(
            String status,
            ConnectionSummaryResponse connections,
            LocksSummary locks,
            RunningQueriesSummary runningQueries,
            List<TopQueryResponse> topQueries,
            List<DatabaseSettingResponse> settings
    ) {
    }

    public record LocksSummary(
            int total,
            int blocked,
            int blocking
    ) {
    }

    public record RunningQueriesSummary(
            int total,
            List<RunningQueryResponse> queries
    ) {
    }

    public record ApplicationSummary(
            SystemMetricsResponse.CpuMetrics cpu,
            SystemMetricsResponse.MemoryMetrics memory,
            SystemMetricsResponse.ThreadMetrics threads,
            long uptimeSeconds
    ) {
    }
}
