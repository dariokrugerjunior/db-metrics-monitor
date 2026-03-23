package br.com.dariokruger.dbmetricsmonitor.intelligence.recommendation;

import br.com.dariokruger.dbmetricsmonitor.intelligence.dto.RecommendationItemResponse;
import java.util.Optional;

public interface RecommendationRule {

    Optional<RecommendationItemResponse> evaluate(RecommendationEvaluationContext context);
}
