package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import java.util.List;

public record ScoreBreakdownResponse(
        String category,
        int score,
        int penaltyPoints,
        List<ScorePenaltyResponse> penalties
) {
}
