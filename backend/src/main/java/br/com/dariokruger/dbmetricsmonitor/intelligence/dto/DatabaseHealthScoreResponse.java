package br.com.dariokruger.dbmetricsmonitor.intelligence.dto;

import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.HealthClassification;
import java.util.List;

public record DatabaseHealthScoreResponse(
        int score,
        HealthClassification classification,
        List<ScorePenaltyResponse> penalties,
        List<ScoreBreakdownResponse> breakdown,
        String summary
) {
}
