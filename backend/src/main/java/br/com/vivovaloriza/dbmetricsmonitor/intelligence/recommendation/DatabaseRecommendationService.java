package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AlertItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.AnomalyItemResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseRecommendationResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatabaseRecommendationService {

    private final List<RecommendationRule> rules;

    public DatabaseRecommendationService(List<RecommendationRule> rules) {
        this.rules = rules;
    }

    public DatabaseRecommendationResponse generate(
            DatabaseHealthSnapshot snapshot,
            List<AlertItemResponse> alerts,
            List<AnomalyItemResponse> anomalies
    ) {
        RecommendationEvaluationContext context = new RecommendationEvaluationContext(snapshot, alerts, anomalies);
        var recommendations = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(java.util.Optional::stream)
                .toList();
        log.info("db_intelligence_recommendations_generated collectedAt={} totalRecommendations={}",
                snapshot.collectedAt(), recommendations.size());
        return new DatabaseRecommendationResponse(snapshot.collectedAt(), recommendations);
    }
}
