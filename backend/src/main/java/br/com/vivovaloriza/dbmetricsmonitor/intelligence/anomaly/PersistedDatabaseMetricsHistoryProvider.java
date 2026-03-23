package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PersistedDatabaseMetricsHistoryProvider implements DatabaseMetricsHistoryProvider {

    private final DatabaseMetricsHistoryRepository repository;
    private final DatabaseIntelligenceProperties properties;

    @Override
    public List<HistoricalMetricsSnapshot> loadBaseline(DatabaseHealthSnapshot snapshot) {
        Instant cutoff = snapshot.collectedAt().minus(properties.getAnomaly().getBaselineWindowMinutes(), ChronoUnit.MINUTES);
        return repository.findByEnvironmentSince(snapshot.environment(), cutoff, snapshot.collectedAt());
    }
}
