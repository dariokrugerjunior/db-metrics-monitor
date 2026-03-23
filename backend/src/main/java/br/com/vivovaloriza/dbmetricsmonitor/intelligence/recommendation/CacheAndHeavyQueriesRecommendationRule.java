package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.RecommendationItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationCode;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.RecommendationPriority;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class CacheAndHeavyQueriesRecommendationRule implements RecommendationRule {

    private final DatabaseIntelligenceProperties properties;

    public CacheAndHeavyQueriesRecommendationRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context) {
        double cacheHit = context.snapshot().cache().cacheHitPercent().doubleValue();
        double maxTopQueryExec = context.snapshot().derivedSignals().maxTopQueryTotalExecTime().doubleValue();
        if (cacheHit >= 99.0d || maxTopQueryExec < properties.getRecommendation().getTopQueryTotalExecTimeThreshold()) {
            return Optional.empty();
        }
        return Optional.of(new RecommendationItemResponse(
                RecommendationCode.REVIEW_INDEX_AND_ACCESS_PATTERN,
                "Revisar indices e padrao de acesso",
                "Cache hit abaixo do alvo combinado com queries caras sugere leitura fisica excessiva ou planos ineficientes.",
                RecommendationPriority.HIGH,
                "A degradacao de cache combinada com alto tempo total de execucao costuma apontar para indice ausente, seletividade ruim ou working set acima do esperado.",
                List.of(
                        "Analise as queries com maior total_exec_time.",
                        "Revise seletividade e cobertura dos indices usados por essas consultas.",
                        "Valide se ha scans amplos recorrentes ou joins sem filtro adequado."
                ),
                List.of("LOW_CACHE_HIT_RATIO", "LONG_RUNNING_QUERY")
        ));
    }
}
