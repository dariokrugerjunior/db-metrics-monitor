package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseAlertResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatabaseAlertEngine {

    private final List<AlertRule> rules;

    public DatabaseAlertEngine(List<AlertRule> rules) {
        this.rules = rules;
    }

    public DatabaseAlertResponse evaluate(DatabaseHealthSnapshot snapshot, List<AnomalyItemResponse> anomalies) {
        AlertEvaluationContext context = new AlertEvaluationContext(snapshot, anomalies);
        var alerts = rules.stream()
                .flatMap(rule -> rule.evaluate(context).stream())
                .toList();
        log.info("db_intelligence_alerts_generated collectedAt={} totalAlerts={}", snapshot.collectedAt(), alerts.size());
        return new DatabaseAlertResponse(snapshot.collectedAt(), alerts.size(), alerts);
    }
}
