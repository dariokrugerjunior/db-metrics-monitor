package br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.DatabaseHealthSnapshotTestData;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.HealthClassification;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.PenaltyCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseHealthScoreServiceTest {

    @Test
    void shouldReturnHealthyScoreWhenSnapshotIsStable() {
        DatabaseIntelligenceProperties properties = new DatabaseIntelligenceProperties();
        DatabaseHealthScoreService service = new DatabaseHealthScoreService(List.of(
                new LockScoreEvaluator(properties),
                new ConnectionScoreEvaluator(properties),
                new QueryScoreEvaluator(properties),
                new CacheScoreEvaluator(properties),
                new ResourceScoreEvaluator(properties),
                new IncidentScoreEvaluator(properties)
        ), properties);

        var response = service.calculate(DatabaseHealthSnapshotTestData.snapshot(
                0, 0, 0, BigDecimal.valueOf(45), 5, BigDecimal.valueOf(99.7), 35, 50, 0, 0
        ));

        assertThat(response.score()).isEqualTo(100);
        assertThat(response.classification()).isEqualTo(HealthClassification.HEALTHY);
        assertThat(response.penalties()).isEmpty();
    }

    @Test
    void shouldApplyPenaltyWhenBlockedLocksExist() {
        DatabaseIntelligenceProperties properties = new DatabaseIntelligenceProperties();
        DatabaseHealthScoreService service = new DatabaseHealthScoreService(List.of(
                new LockScoreEvaluator(properties),
                new ConnectionScoreEvaluator(properties),
                new QueryScoreEvaluator(properties),
                new CacheScoreEvaluator(properties),
                new ResourceScoreEvaluator(properties),
                new IncidentScoreEvaluator(properties)
        ), properties);

        var response = service.calculate(DatabaseHealthSnapshotTestData.snapshot(
                2, 0, 0, BigDecimal.valueOf(45), 5, BigDecimal.valueOf(99.7), 35, 50, 0, 0
        ));

        assertThat(response.score()).isEqualTo(75);
        assertThat(response.penalties()).extracting("code").contains(PenaltyCode.BLOCKED_LOCKS);
    }
}
