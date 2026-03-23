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
@Order(30)
public class QueryScoreEvaluator extends AbstractScoreCategoryEvaluator implements ScoreCategoryEvaluator {

    private final DatabaseIntelligenceProperties properties;

    public QueryScoreEvaluator(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot) {
        List<ScorePenaltyResponse> penalties = new ArrayList<>();
        long maxRunningQuerySeconds = snapshot.runningQueries().maxRunningQuerySeconds();
        if (maxRunningQuerySeconds > properties.getScore().getLongQueryCriticalSeconds()) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.CRITICAL_LONG_RUNNING_QUERY,
                    properties.getScore().getPenalties().getCriticalLongRunningQuery(),
                    "Ha queries longas acima do limite critico."
            ));
        } else if (maxRunningQuerySeconds > properties.getScore().getLongQueryWarningSeconds()) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.LONG_RUNNING_QUERY,
                    properties.getScore().getPenalties().getLongRunningQuery(),
                    "Ha queries longas acima do limite de atencao."
            ));
        }
        return breakdown("QUERY", penalties);
    }
}
