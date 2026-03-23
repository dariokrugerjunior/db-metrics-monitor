package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCategory;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertSeverity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
public class IncidentAlertRule implements AlertRule {

    private final DatabaseIntelligenceProperties properties;

    public IncidentAlertRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        int totalIncidents = context.snapshot().historicalIncidents().totalIncidentsLast24Hours();
        int lockIncidents = context.snapshot().historicalIncidents().lockIncidentsLast24Hours();
        if (lockIncidents >= properties.getAlerts().getRecurringLockIncidentThreshold()
                || totalIncidents >= properties.getAlerts().getRecurringIncidentThreshold()) {
            AlertSeverity severity = lockIncidents >= properties.getAlerts().getRecurringLockIncidentThreshold()
                    ? AlertSeverity.CRITICAL
                    : AlertSeverity.WARNING;
            alerts.add(new AlertItemResponse(
                    AlertCode.INCIDENT_RECURRENT_PATTERN,
                    "Padrao recorrente de incidentes",
                    "Historico recente mostra " + totalIncidents + " incidentes, sendo " + lockIncidents + " ligados a lock.",
                    severity,
                    AlertCategory.INCIDENT,
                    context.snapshot().collectedAt(),
                    "Planeje uma correcao estrutural para a causa repetitiva, nao apenas acao operacional."
            ));
        }
        return alerts;
    }
}
