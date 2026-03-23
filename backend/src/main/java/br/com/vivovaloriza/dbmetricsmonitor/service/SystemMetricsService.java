package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.dto.SystemMetricsResponse;
import com.sun.management.OperatingSystemMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicReference<Double> lastKnownCpuPercent = new AtomicReference<>(0.0d);

    public SystemMetricsResponse collect() {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        SystemMetricsResponse.MemoryMetrics memory = new SystemMetricsResponse.MemoryMetrics(
                heap.getUsed(),
                heap.getMax(),
                nonHeap.getUsed(),
                nonHeap.getCommitted(),
                runtime.totalMemory(),
                runtime.freeMemory()
        );

        SystemMetricsResponse.CpuMetrics cpu = new SystemMetricsResponse.CpuMetrics(
                resolveCpuPercent(),
                meterValue("system.load.average.1m"),
                runtime.availableProcessors()
        );

        SystemMetricsResponse.ThreadMetrics threads = new SystemMetricsResponse.ThreadMetrics(
                threadMXBean.getThreadCount(),
                threadMXBean.getDaemonThreadCount(),
                threadMXBean.getPeakThreadCount()
        );

        return new SystemMetricsResponse(memory, cpu, threads, java.time.Duration.ofMillis(runtimeMXBean.getUptime()));
    }

    private double meterValue(String meterName) {
        Gauge gauge = meterRegistry.find(meterName).gauge();
        return gauge != null ? gauge.value() : 0.0d;
    }

    private double resolveCpuPercent() {
        double micrometerPercent = normalizeCpuMetric(meterValue("process.cpu.usage"));
        if (micrometerPercent > 0.0d) {
            lastKnownCpuPercent.set(micrometerPercent);
            return micrometerPercent;
        }

        double osBeanPercent = osBeanCpuPercent();
        if (osBeanPercent > 0.0d) {
            lastKnownCpuPercent.set(osBeanPercent);
            return osBeanPercent;
        }

        return lastKnownCpuPercent.get();
    }

    private double normalizeCpuMetric(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0d) {
            return 0.0d;
        }

        double percent = value <= 1.0d ? value * 100.0d : value;
        return Math.min(percent, 100.0d);
    }

    private double osBeanCpuPercent() {
        var operatingSystemMxBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMxBean instanceof OperatingSystemMXBean osBean) {
            return normalizeCpuMetric(osBean.getProcessCpuLoad());
        }
        return 0.0d;
    }
}
