package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCategory;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertSeverity;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalyCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AnomalySeverity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class AnomalyAlertRule implements AlertRule {

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        context.anomalies().stream()
                .filter(anomaly -> anomaly.code() == AnomalyCode.TOTAL_CONNECTIONS_SPIKE || anomaly.code() == AnomalyCode.ACTIVE_CONNECTIONS_SPIKE)
                .findFirst()
                .ifPresent(anomaly -> alerts.add(new AlertItemResponse(
                        AlertCode.ANOMALY_CONNECTION_SPIKE,
                        "Anomalia de conexoes",
                        anomaly.message(),
                        mapSeverity(anomaly.severity()),
                        AlertCategory.ANOMALY,
                        context.snapshot().collectedAt(),
                        "Compare o pico atual com deploys, jobs ou mudancas de trafego recentes."
                )));

        context.anomalies().stream()
                .filter(anomaly -> anomaly.code() == AnomalyCode.BLOCKED_LOCK_SPIKE)
                .findFirst()
                .ifPresent(anomaly -> alerts.add(new AlertItemResponse(
                        AlertCode.ANOMALY_LOCK_SPIKE,
                        "Anomalia de locks",
                        anomaly.message(),
                        mapSeverity(anomaly.severity()),
                        AlertCategory.ANOMALY,
                        context.snapshot().collectedAt(),
                        "Valide sessoes bloqueadoras e alteracoes recentes que ampliaram a contencao."
                )));
        return alerts;
    }

    private AlertSeverity mapSeverity(AnomalySeverity severity) {
        return severity == AnomalySeverity.CRITICAL ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }
}
