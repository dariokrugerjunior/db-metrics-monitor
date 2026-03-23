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
@Order(30)
public class QueryAlertRule implements AlertRule {

    private final DatabaseIntelligenceProperties properties;

    public QueryAlertRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        long maxDuration = context.snapshot().runningQueries().maxRunningQuerySeconds();
        if (maxDuration > properties.getAlerts().getLongQueryCriticalSeconds()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.LONG_RUNNING_QUERY,
                    "Query longa critica",
                    "Ha query em execucao ha " + maxDuration + " segundos.",
                    AlertSeverity.CRITICAL,
                    AlertCategory.QUERY,
                    context.snapshot().collectedAt(),
                    "Inspecione o plano de execucao e a dependencia de locks antes de cancelar."
            ));
        } else if (maxDuration > properties.getAlerts().getLongQueryWarningSeconds()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.LONG_RUNNING_QUERY,
                    "Query longa detectada",
                    "Ha query em execucao ha " + maxDuration + " segundos.",
                    AlertSeverity.WARNING,
                    AlertCategory.QUERY,
                    context.snapshot().collectedAt(),
                    "Verifique se a consulta esta dentro do SLA esperado e se depende de lock."
            ));
        }
        return alerts;
    }
}
