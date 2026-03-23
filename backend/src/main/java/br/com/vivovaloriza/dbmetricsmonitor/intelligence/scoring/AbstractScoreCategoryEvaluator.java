package br.com.vivovaloriza.dbmetricsmonitor.intelligence.scoring;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScoreBreakdownResponse;
import br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto.ScorePenaltyResponse;
import java.util.List;

abstract class AbstractScoreCategoryEvaluator {

    protected ScoreBreakdownResponse breakdown(String category, List<ScorePenaltyResponse> penalties) {
        int totalPenalty = penalties.stream().mapToInt(ScorePenaltyResponse::points).sum();
        return new ScoreBreakdownResponse(category, Math.max(0, 100 - totalPenalty), totalPenalty, penalties);
    }
}
