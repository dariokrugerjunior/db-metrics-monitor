package br.com.vivovaloriza.dbmetricsmonitor.intelligence.alerts;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCategory;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.AlertSeverity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class LockAlertRule implements AlertRule {

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        if (context.snapshot().locks().blockedLocks() > 0) {
            alerts.add(new AlertItemResponse(
                    AlertCode.LOCK_BLOCKED_SESSION,
                    "Sessao bloqueada detectada",
                    "Existem " + context.snapshot().locks().blockedLocks() + " locks bloqueados neste momento.",
                    AlertSeverity.CRITICAL,
                    AlertCategory.LOCK,
                    context.snapshot().collectedAt(),
                    "Investigue a sessao bloqueadora e avalie a transacao aberta associada."
            ));
        }
        if (context.snapshot().locks().blockingLocks() > 0) {
            alerts.add(new AlertItemResponse(
                    AlertCode.LOCK_BLOCKING_SESSION,
                    "Sessao bloqueadora detectada",
                    "Existem " + context.snapshot().locks().blockingLocks() + " sessoes bloqueando outras operacoes.",
                    AlertSeverity.WARNING,
                    AlertCategory.LOCK,
                    context.snapshot().collectedAt(),
                    "Mapeie a sessao raiz e confirme se ela ainda precisa manter o lock."
            ));
        }
        return alerts;
    }
}
