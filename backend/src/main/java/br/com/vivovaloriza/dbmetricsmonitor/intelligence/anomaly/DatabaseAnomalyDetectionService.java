package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyDetectionResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.MetricBaselineResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalyCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalySeverity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatabaseAnomalyDetectionService {

    private final DatabaseMetricsHistoryProvider historyProvider;
    private final DatabaseIntelligenceProperties properties;

    public DatabaseAnomalyDetectionService(DatabaseMetricsHistoryProvider historyProvider, DatabaseIntelligenceProperties properties) {
        this.historyProvider = historyProvider;
        this.properties = properties;
    }

    public AnomalyDetectionResponse detect(DatabaseHealthSnapshot snapshot) {
        List<HistoricalMetricsSnapshot> history = historyProvider.loadBaseline(snapshot);
        if (history.size() < properties.getAnomaly().getMinimumSamples()) {
            String message = "Baseline insuficiente para detectar anomalias com confianca.";
            log.info("db_intelligence_anomaly_baseline_insufficient collectedAt={} samples={} required={}",
                    snapshot.collectedAt(), history.size(), properties.getAnomaly().getMinimumSamples());
            return new AnomalyDetectionResponse(snapshot.collectedAt(), false, message, List.of());
        }

        List<AnomalyItemResponse> anomalies = new ArrayList<>();
        addPercentSpike(anomalies, AnomalyCode.TOTAL_CONNECTIONS_SPIKE, "total_connections",
                snapshot.connections().totalConnections(), history.stream().mapToDouble(HistoricalMetricsSnapshot::totalConnections).boxed().toList(),
                properties.getAnomaly().getTotalConnectionsPercentThreshold(), AnomalySeverity.WARNING);
        addPercentSpike(anomalies, AnomalyCode.ACTIVE_CONNECTIONS_SPIKE, "active_connections",
                snapshot.connections().activeConnections(), history.stream().mapToDouble(HistoricalMetricsSnapshot::activeConnections).boxed().toList(),
                properties.getAnomaly().getConnectionActivePercentThreshold(), AnomalySeverity.WARNING);
        addPercentSpike(anomalies, AnomalyCode.IDLE_IN_TRANSACTION_SPIKE, "idle_in_transaction",
                snapshot.connections().idleInTransactionConnections(), history.stream().mapToDouble(HistoricalMetricsSnapshot::idleInTransactionConnections).boxed().toList(),
                properties.getAnomaly().getIdleInTransactionPercentThreshold(), AnomalySeverity.WARNING);
        addBlockedLockSpike(anomalies, snapshot.locks().blockedLocks(),
                history.stream().mapToDouble(HistoricalMetricsSnapshot::blockedLocks).boxed().toList());
        addPercentSpike(anomalies, AnomalyCode.RUNNING_QUERIES_SPIKE, "running_queries",
                snapshot.runningQueries().runningQueries(), history.stream().mapToDouble(HistoricalMetricsSnapshot::runningQueries).boxed().toList(),
                properties.getAnomaly().getRunningQueriesPercentThreshold(), AnomalySeverity.WARNING);
        addPercentSpike(anomalies, AnomalyCode.LONG_RUNNING_QUERIES_SPIKE, "long_running_queries",
                snapshot.runningQueries().longRunningQueries(), history.stream().mapToDouble(HistoricalMetricsSnapshot::longRunningQueries).boxed().toList(),
                properties.getAnomaly().getLongRunningQueriesPercentThreshold(), AnomalySeverity.WARNING);
        addCacheDrop(anomalies, snapshot.cache().cacheHitPercent().doubleValue(),
                history.stream().mapToDouble(HistoricalMetricsSnapshot::cacheHitPercent).boxed().toList());
        addPercentSpike(anomalies, AnomalyCode.CPU_SPIKE, "cpu",
                snapshot.cpu().percent(), history.stream().mapToDouble(HistoricalMetricsSnapshot::cpuPercent).boxed().toList(),
                properties.getAnomaly().getCpuPercentThreshold(), AnomalySeverity.WARNING);
        addPercentSpike(anomalies, AnomalyCode.MEMORY_SPIKE, "memory",
                snapshot.memory().percent(), history.stream().mapToDouble(HistoricalMetricsSnapshot::memoryPercent).boxed().toList(),
                properties.getAnomaly().getMemoryPercentThreshold(), AnomalySeverity.WARNING);

        log.info("db_intelligence_anomalies_detected collectedAt={} totalAnomalies={}", snapshot.collectedAt(), anomalies.size());
        return new AnomalyDetectionResponse(snapshot.collectedAt(), true, "Baseline historica aplicada com sucesso.", anomalies);
    }

    private void addBlockedLockSpike(List<AnomalyItemResponse> anomalies, double currentValue, List<Double> values) {
        MetricBaselineResponse baseline = baseline("blocked_locks", currentValue, values);
        if (baseline.average() == 0.0d && currentValue > 0.0d) {
            anomalies.add(new AnomalyItemResponse(
                    AnomalyCode.BLOCKED_LOCK_SPIKE,
                    AnomalySeverity.CRITICAL,
                    "Locks bloqueados surgiram fora do padrao historico recente.",
                    baseline
            ));
            return;
        }
        if (baseline.percentDeviation() > properties.getAnomaly().getBlockedLocksPercentThreshold()) {
            anomalies.add(new AnomalyItemResponse(
                    AnomalyCode.BLOCKED_LOCK_SPIKE,
                    AnomalySeverity.CRITICAL,
                    "Locks bloqueados acima do comportamento historico esperado.",
                    baseline
            ));
        }
    }

    private void addCacheDrop(List<AnomalyItemResponse> anomalies, double currentValue, List<Double> values) {
        MetricBaselineResponse baseline = baseline("cache_hit", currentValue, values);
        double absoluteDrop = baseline.average() - currentValue;
        if (absoluteDrop >= properties.getAnomaly().getCacheHitDropThreshold()) {
            anomalies.add(new AnomalyItemResponse(
                    AnomalyCode.CACHE_HIT_DROP,
                    absoluteDrop >= properties.getAnomaly().getCacheHitDropThreshold() * 2 ? AnomalySeverity.CRITICAL : AnomalySeverity.WARNING,
                    "Cache hit ratio caiu de forma relevante frente a baseline recente.",
                    baseline
            ));
        }
    }

    private void addPercentSpike(
            List<AnomalyItemResponse> anomalies,
            AnomalyCode code,
            String metric,
            double currentValue,
            List<Double> values,
            double threshold,
            AnomalySeverity baseSeverity
    ) {
        MetricBaselineResponse baseline = baseline(metric, currentValue, values);
        if (baseline.average() == 0.0d) {
            if (currentValue > 0.0d) {
                anomalies.add(new AnomalyItemResponse(code, baseSeverity, "Valor atual fora do padrao historico nulo para " + metric + ".", baseline));
            }
            return;
        }
        if (baseline.percentDeviation() > threshold) {
            AnomalySeverity severity = baseline.percentDeviation() > threshold * 2 ? AnomalySeverity.CRITICAL : baseSeverity;
            anomalies.add(new AnomalyItemResponse(
                    code,
                    severity,
                    "Desvio relevante detectado para " + metric + " frente a media historica.",
                    baseline
            ));
        }
    }

    private MetricBaselineResponse baseline(String metric, double currentValue, List<Double> values) {
        List<Double> ordered = values.stream().sorted(Comparator.naturalOrder()).toList();
        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
        double median = ordered.size() % 2 == 0
                ? (ordered.get((ordered.size() / 2) - 1) + ordered.get(ordered.size() / 2)) / 2.0d
                : ordered.get(ordered.size() / 2);
        double min = ordered.get(0);
        double max = ordered.get(ordered.size() - 1);
        double percentDeviation = average == 0.0d
                ? (currentValue > 0.0d ? 100.0d : 0.0d)
                : ((currentValue - average) / average) * 100.0d;
        return new MetricBaselineResponse(metric, currentValue, average, median, min, max, percentDeviation);
    }
}
