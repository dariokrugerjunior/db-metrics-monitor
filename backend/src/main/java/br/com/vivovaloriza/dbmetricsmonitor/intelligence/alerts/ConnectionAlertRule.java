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
@Order(20)
public class ConnectionAlertRule implements AlertRule {

    private final DatabaseIntelligenceProperties properties;

    public ConnectionAlertRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        double usagePercent = context.snapshot().connections().usagePercent().doubleValue();
        if (usagePercent > properties.getAlerts().getConnectionCriticalPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.HIGH_CONNECTION_USAGE,
                    "Uso critico de conexoes",
                    "Uso atual de conexoes em " + usagePercent + "% do limite configurado.",
                    AlertSeverity.CRITICAL,
                    AlertCategory.CONNECTION,
                    context.snapshot().collectedAt(),
                    "Ajuste o pool de conexoes ou reduza sessoes ociosas imediatamente."
            ));
        } else if (usagePercent > properties.getAlerts().getConnectionWarningPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.HIGH_CONNECTION_USAGE,
                    "Uso elevado de conexoes",
                    "Uso atual de conexoes em " + usagePercent + "% do limite configurado.",
                    AlertSeverity.WARNING,
                    AlertCategory.CONNECTION,
                    context.snapshot().collectedAt(),
                    "Revise o pool de conexoes e identifique sessoes que podem ser recicladas."
            ));
        }

        if (context.snapshot().connections().idleInTransactionConnections() >= properties.getAlerts().getIdleInTransactionWarningCount()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.IDLE_IN_TRANSACTION_DETECTED,
                    "Idle in transaction detectado",
                    "Foram encontradas " + context.snapshot().connections().idleInTransactionConnections() + " sessoes idle in transaction.",
                    AlertSeverity.WARNING,
                    AlertCategory.CONNECTION,
                    context.snapshot().collectedAt(),
                    "Confirme se a aplicacao esta encerrando transacoes explicitamente."
            ));
        }
        return alerts;
    }
}
