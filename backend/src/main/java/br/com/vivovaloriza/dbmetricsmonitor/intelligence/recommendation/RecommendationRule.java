package br.com.vivovaloriza.dbmetricsmonitor.intelligence.recommendation;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.RecommendationItemResponse;
import java.util.Optional;

public interface RecommendationRule {

    Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context);
}
