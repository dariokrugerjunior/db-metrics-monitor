package br.com.vivovaloriza.dbmetricsmonitor.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMetricsServiceTest {

    @Test
    void shouldCollectJvmMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AtomicReference<Double> cpuUsage = new AtomicReference<>(0.42d);
        AtomicReference<Double> loadAverage = new AtomicReference<>(1.75d);
        meterRegistry.gauge("process.cpu.usage", cpuUsage, AtomicReference::get);
        meterRegistry.gauge("system.load.average.1m", loadAverage, AtomicReference::get);

        SystemMetricsService service = new SystemMetricsService(meterRegistry);

        var result = service.collect();

        assertThat(result.memory().heapUsedBytes()).isNotNegative();
        assertThat(result.cpu().processCpuUsage()).isEqualTo(42.0d);
        assertThat(result.cpu().systemLoadAverage()).isEqualTo(1.75d);
        assertThat(result.threads().liveThreads()).isPositive();
        assertThat(result.uptime()).isNotNull();
    }
}
