package br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.config.DatabaseIntelligenceProperties;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.DatabaseHealthSnapshot;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScoreBreakdownResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScorePenaltyResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.PenaltyCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class CacheScoreEvaluator extends AbstractScoreCategoryEvaluator implements ScoreCategoryEvaluator {

    private final DatabaseIntelligenceProperties properties;

    public CacheScoreEvaluator(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot) {
        List<ScorePenaltyResponse> penalties = new ArrayList<>();
        double cacheHit = snapshot.cache().cacheHitPercent().doubleValue();
        if (cacheHit < 97.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.CRITICAL_CACHE_HIT_RATIO,
                    properties.getScore().getPenalties().getCriticalCacheHit(),
                    "Cache hit ratio abaixo de 97%."
            ));
        } else if (cacheHit < 99.0d) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.LOW_CACHE_HIT_RATIO,
                    properties.getScore().getPenalties().getLowCacheHit(),
                    "Cache hit ratio abaixo de 99%."
            ));
        }
        return breakdown("CACHE", penalties);
    }
}
