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
@Order(40)
public class CacheAlertRule implements AlertRule {

    private final DatabaseIntelligenceProperties properties;

    public CacheAlertRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<AlertItemResponse> evaluate(AlertEvaluationContext context) {
        List<AlertItemResponse> alerts = new ArrayList<>();
        double cacheHit = context.snapshot().cache().cacheHitPercent().doubleValue();
        if (cacheHit < properties.getAlerts().getCacheCriticalPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.LOW_CACHE_HIT_RATIO,
                    "Cache hit ratio critico",
                    "Cache hit ratio em " + cacheHit + "%, abaixo do limite critico.",
                    AlertSeverity.CRITICAL,
                    AlertCategory.CACHE,
                    context.snapshot().collectedAt(),
                    "Avalie indices, working set e consultas que leem muitos blocos fisicos."
            ));
        } else if (cacheHit < properties.getAlerts().getCacheWarningPercent()) {
            alerts.add(new AlertItemResponse(
                    AlertCode.LOW_CACHE_HIT_RATIO,
                    "Cache hit ratio abaixo do ideal",
                    "Cache hit ratio em " + cacheHit + "%, abaixo do alvo operacional.",
                    AlertSeverity.WARNING,
                    AlertCategory.CACHE,
                    context.snapshot().collectedAt(),
                    "Revise padroes de acesso e confirme se o buffer cache suporta a carga."
            ));
        }
        return alerts;
    }
}
