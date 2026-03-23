package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMetricsHistoryRecorder {

    private final DatabaseMetricsHistoryRepository repository;

    public void record(DatabaseHealthSnapshot snapshot) {
        repository.save(snapshot);
        log.info("db_intelligence_baseline_recorded collectedAt={} environment={} totalConnections={} blockedLocks={}",
                snapshot.collectedAt(),
                snapshot.environment(),
                snapshot.connections().totalConnections(),
                snapshot.locks().blockedLocks());
    }
}
