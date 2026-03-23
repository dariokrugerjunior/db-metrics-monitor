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
@Order(50)
public class RecurrentLockIncidentRecommendationRule implements RecommendationRule {

    private final DatabaseIntelligenceProperties properties;

    public RecurrentLockIncidentRecommendationRule(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context) {
        if (context.snapshot().historicalIncidents().lockIncidentsLast24Hours()
                < properties.getRecommendation().getLockIncidentRecurrenceThreshold()) {
            return Optional.empty();
        }
        return Optional.of(new RecommendationItemResponse(
                RecommendationCode.ADDRESS_RECURRENT_LOCK_PATTERN,
                "Atuar estruturalmente sobre recorrencia de locks",
                "O historico mostra repeticao de incidentes de lock, o que pede correcao estrutural.",
                RecommendationPriority.HIGH,
                "Recorrencia de lock costuma sinalizar disputa previsivel entre workloads, e nao um evento isolado.",
                List.of(
                        "Mapeie quais transacoes e tabelas aparecem com frequencia nos incidentes.",
                        "Reduza tempo de retencao de lock nas rotinas envolvidas.",
                        "Considere reordenacao de acesso, particionamento ou ajuste de batch."
                ),
                List.of("INCIDENT_RECURRENT_PATTERN", "LOCK_BLOCKED_SESSION")
        ));
    }
}
