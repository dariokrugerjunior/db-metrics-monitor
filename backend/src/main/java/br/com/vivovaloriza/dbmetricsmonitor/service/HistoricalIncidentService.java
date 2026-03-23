package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DashboardSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.HistoricalIncidentSummaryResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.LockInfoResponse;
import br.com.vivovaloriza.dbmetricsmonitor.repository.HistoricalIncidentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HistoricalIncidentService {

    private final HistoricalIncidentRepository repository;
    private final AppProperties appProperties;

    public void recordSnapshotIncidents(DashboardSummaryResponse summary, List<LockInfoResponse> blockedLocks, Instant createdAt) {
        double cpuPercent = round(summary.application().cpu().processCpuUsage());
        double memoryPercent = summary.application().memory().jvmTotalMemoryBytes() > 0
                ? round((summary.application().memory().heapUsedBytes() * 100.0d) / summary.application().memory().jvmTotalMemoryBytes())
                : 0.0d;

        if (cpuPercent >= appProperties.getHistory().getCpuThresholdPercent()) {
            repository.saveIncident(
                    "CPU_HIGH",
                    cpuPercent >= 85.0d ? "critical" : "warning",
                    "CPU acima do limite",
                    "Snapshot registrou CPU acima do threshold configurado.",
                    cpuPercent,
                    "%",
                    "application",
                    "processCpuUsage",
                    createdAt
            );
        }

        if (memoryPercent >= appProperties.getHistory().getMemoryThresholdPercent()) {
            repository.saveIncident(
                    "MEMORY_HIGH",
                    memoryPercent >= 85.0d ? "critical" : "warning",
                    "Memória acima do limite",
                    "Snapshot registrou uso de memória acima do threshold configurado.",
                    memoryPercent,
                    "%",
                    "application",
                    "heapUsage",
                    createdAt
            );
        }

        blockedLocks.stream()
                .filter(lock -> lock.relation() != null && !lock.relation().isBlank())
                .forEach(lock -> repository.saveIncident(
                        "LOCK_BLOCKING",
                        "critical",
                        "Lock bloqueando tabela",
                        "Sessão bloqueada detectada na tabela " + lock.relation(),
                        null,
                        null,
                        "postgres",
                        lock.relation(),
                        createdAt
                ));
    }

    public List<HistoricalIncidentResponse> getRecentIncidents(Integer limit) {
        int safeLimit = limit == null ? appProperties.getHistory().getRecentLimit() : limit;
        return repository.findRecentIncidents(Math.min(Math.max(safeLimit, 1), 1000));
    }

    public HistoricalIncidentSummaryResponse getSummary() {
        return repository.summarize();
    }

    public HistoricalIncidentSummaryResponse getSummaryLast24Hours() {
        return repository.summarizeSince(Instant.now().minusSeconds(24 * 60 * 60));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
