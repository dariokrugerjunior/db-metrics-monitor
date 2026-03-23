package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Duration;

public record SystemMetricsResponse(
        MemoryMetrics memory,
        CpuMetrics cpu,
        ThreadMetrics threads,
        Duration uptime
) {
    public record MemoryMetrics(
            long heapUsedBytes,
            long heapMaxBytes,
            long nonHeapUsedBytes,
            long nonHeapCommittedBytes,
            long jvmTotalMemoryBytes,
            long jvmFreeMemoryBytes
    ) {
    }

    public record CpuMetrics(
            double processCpuUsage,
            double systemLoadAverage,
            int availableProcessors
    ) {
    }

    public record ThreadMetrics(
            int liveThreads,
            int daemonThreads,
            long peakThreads
    ) {
    }
}
