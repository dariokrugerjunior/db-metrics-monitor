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
@Order(10)
public class LockScoreEvaluator extends AbstractScoreCategoryEvaluator implements ScoreCategoryEvaluator {

    private final DatabaseIntelligenceProperties properties;

    public LockScoreEvaluator(DatabaseIntelligenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public ScoreBreakdownResponse evaluate(DatabaseHealthSnapshot snapshot) {
        List<ScorePenaltyResponse> penalties = new ArrayList<>();
        if (snapshot.locks().blockedLocks() > 0) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.BLOCKED_LOCKS,
                    properties.getScore().getPenalties().getBlockedLocks(),
                    "Ha locks bloqueados no momento."
            ));
        }
        if (snapshot.locks().blockingLocks() > 0) {
            penalties.add(new ScorePenaltyResponse(
                    PenaltyCode.BLOCKING_LOCKS,
                    properties.getScore().getPenalties().getBlockingLocks(),
                    "Ha sessoes bloqueadoras em execucao."
            ));
        }
        return breakdown("LOCK", penalties);
    }
}
