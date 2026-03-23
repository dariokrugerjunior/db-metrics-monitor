package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.DatabaseHealthSnapshotTestData;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertSeverity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseAlertEngineTest {

    @Test
    void shouldGenerateHighConnectionAlert() {
        DatabaseAlertEngine engine = new DatabaseAlertEngine(List.of(new ConnectionAlertRule(new DatabaseIntelligenceProperties())));

        var response = engine.evaluate(
                DatabaseHealthSnapshotTestData.snapshot(0, 0, 0, BigDecimal.valueOf(91), 5, BigDecimal.valueOf(99.5), 20, 30, 0, 0),
                List.of()
        );

        assertThat(response.alerts()).hasSize(1);
        assertThat(response.alerts().getFirst().code()).isEqualTo(AlertCode.HIGH_CONNECTION_USAGE);
        assertThat(response.alerts().getFirst().severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void shouldGenerateIdleInTransactionAlert() {
        DatabaseAlertEngine engine = new DatabaseAlertEngine(List.of(new ConnectionAlertRule(new DatabaseIntelligenceProperties())));

        var response = engine.evaluate(
                DatabaseHealthSnapshotTestData.snapshot(0, 0, 2, BigDecimal.valueOf(40), 5, BigDecimal.valueOf(99.5), 20, 30, 0, 0),
                List.of()
        );

        assertThat(response.alerts()).extracting("code").contains(AlertCode.IDLE_IN_TRANSACTION_DETECTED);
    }
}
