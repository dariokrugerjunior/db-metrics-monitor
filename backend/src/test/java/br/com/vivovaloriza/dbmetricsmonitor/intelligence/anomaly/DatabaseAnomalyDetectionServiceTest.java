package br.com.vivovaloriza.dbmetricsmonitor.intelligence.anomaly;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.DatabaseHealthSnapshotTestData;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DatabaseAnomalyDetectionServiceTest {

    @Test
    void shouldDetectConnectionSpike() {
        DatabaseMetricsHistoryProvider provider = Mockito.mock(DatabaseMetricsHistoryProvider.class);
        DatabaseIntelligenceProperties properties = new DatabaseIntelligenceProperties();
        DatabaseAnomalyDetectionService service = new DatabaseAnomalyDetectionService(provider, properties);

        var snapshot = DatabaseHealthSnapshotTestData.snapshot(
                0, 0, 0, BigDecimal.valueOf(70), 5, BigDecimal.valueOf(99.5), 20, 30, 0, 0
        );
        when(provider.loadBaseline(snapshot)).thenReturn(List.of(
                new HistoricalMetricsSnapshot(Instant.parse("2026-03-23T10:00:00Z"), 20, 8, 0, 0, 2, 0, 99.6, 20, 30),
                new HistoricalMetricsSnapshot(Instant.parse("2026-03-23T10:20:00Z"), 20, 10, 0, 0, 2, 0, 99.7, 22, 31),
                new HistoricalMetricsSnapshot(Instant.parse("2026-03-23T10:40:00Z"), 19, 9, 0, 0, 3, 0, 99.6, 21, 30),
                new HistoricalMetricsSnapshot(Instant.parse("2026-03-23T11:00:00Z"), 21, 10, 0, 0, 2, 0, 99.7, 20, 29),
                new HistoricalMetricsSnapshot(Instant.parse("2026-03-23T11:20:00Z"), 20, 9, 0, 0, 3, 0, 99.6, 19, 30),
                new HistoricalMetricsSnapshot(Instant.parse("2026-03-23T11:40:00Z"), 20, 10, 0, 0, 2, 0, 99.6, 20, 30)
        ));

        var response = service.detect(snapshot);

        assertThat(response.baselineAvailable()).isTrue();
        assertThat(response.anomalies()).extracting("code").contains(AnomalyCode.ACTIVE_CONNECTIONS_SPIKE);
    }
}
