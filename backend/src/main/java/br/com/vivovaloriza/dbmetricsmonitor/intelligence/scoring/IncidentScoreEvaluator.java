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
@Order(60)
public class IncidentScoreEvaluator extends AbstractScoreCategoryEvaluator implements ScoreCategoryEvaluator {

    private final DatabaseIntelligenceProperties properties;

    public IncidentScoreEvaluator(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot) {
        List<ScorePenaltyResponse> penalties = new ArrayList<>();
        if (snapshot.historicalIncidents().totalIncidentsLast24Hours() >= properties.getScore().getRecurringIncidentThreshold()) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.RECURRENT_INCIDENTS,
                    properties.getScore().getPenalties().getRecurringIncidents(),
                    "Ha recorrencia recente de incidentes operacionais."
            ));
        }
        if (snapshot.historicalIncidents().lockIncidentsLast24Hours() >= properties.getScore().getRecurringLockIncidentThreshold()) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.RECURRENT_LOCK_INCIDENTS,
                    properties.getScore().getPenalties().getRecurringLockIncidents(),
                    "Ha recorrencia recente de incidentes de lock."
            ));
        }
        return breakdown("INCIDENT", penalties);
    }
}
