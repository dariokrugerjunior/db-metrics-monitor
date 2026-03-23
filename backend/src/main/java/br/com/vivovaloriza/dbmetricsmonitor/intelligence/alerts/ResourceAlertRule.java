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
@Order(50)
public class ResourceAlertRule implements AlertRule {

    private final DatabaseIntelligenceProperties properties;

    public ResourceAlertRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        double cpu = context.snapshot().cpu().percent();
        if (cpu > properties.getAlerts().getCpuCriticalPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.HIGH_CPU_USAGE,
                    "CPU critica",
                    "CPU atual em " + cpu + "%.",
                    AlertSeverity.CRITICAL,
                    AlertCategory.CPU,
                    context.snapshot().collectedAt(),
                    "Correlacione picos de CPU com queries mais custosas e concorrencia."
            ));
        } else if (cpu > properties.getAlerts().getCpuWarningPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.HIGH_CPU_USAGE,
                    "CPU elevada",
                    "CPU atual em " + cpu + "%.",
                    AlertSeverity.WARNING,
                    AlertCategory.CPU,
                    context.snapshot().collectedAt(),
                    "Revise consultas caras, paralelismo e carga concorrente."
            ));
        }

        double memory = context.snapshot().memory().percent();
        if (memory > properties.getAlerts().getMemoryCriticalPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.HIGH_MEMORY_USAGE,
                    "Memoria critica",
                    "Memoria atual em " + memory + "%.",
                    AlertSeverity.CRITICAL,
                    AlertCategory.MEMORY,
                    context.snapshot().collectedAt(),
                    "Verifique working set, concorrencia e configuracoes de memoria."
            ));
        } else if (memory > properties.getAlerts().getMemoryWarningPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.HIGH_MEMORY_USAGE,
                    "Memoria elevada",
                    "Memoria atual em " + memory + "%.",
                    AlertSeverity.WARNING,
                    AlertCategory.MEMORY,
                    context.snapshot().collectedAt(),
                    "Observe crescimento de heap e relacao com carga de consultas."
            ));
        }
        return alerts;
    }
}
