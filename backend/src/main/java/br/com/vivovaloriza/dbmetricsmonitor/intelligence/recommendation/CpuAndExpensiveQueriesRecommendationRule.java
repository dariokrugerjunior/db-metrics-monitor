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
@Order(40)
public class CpuAndExpensiveQueriesRecommendationRule implements RecommendationRule {

    private final DatabaseIntelligenceProperties properties;

    public CpuAndExpensiveQueriesRecommendationRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context) {
        double maxTopQueryExec = context.snapshot().derivedSignals().maxTopQueryTotalExecTime().doubleValue();
        if (context.snapshot().cpu().percent() <= 75.0d
                || maxTopQueryExec < properties.getRecommendation().getTopQueryTotalExecTimeThreshold()) {
            return Optional.empty();
        }
        return Optional.of(new RecommendationItemResponse(
                RecommendationCode.ANALYZE_EXPENSIVE_QUERIES,
                "Analisar queries mais custosas",
                "CPU alta junto com total_exec_time elevado nas top queries indica consumo intensivo de processamento por SQL.",
                RecommendationPriority.HIGH,
                "A correlacao entre CPU e consultas caras e um forte indicador de gargalo na camada SQL, nao apenas no host.",
                List.of(
                        "Liste as queries mais caras por total_exec_time e mean_exec_time.",
                        "Compare o plano atual com historico e parametros relevantes.",
                        "Priorize tuning das consultas com maior impacto acumulado."
                ),
                List.of("HIGH_CPU_USAGE", "LONG_RUNNING_QUERY")
        ));
    }
}
