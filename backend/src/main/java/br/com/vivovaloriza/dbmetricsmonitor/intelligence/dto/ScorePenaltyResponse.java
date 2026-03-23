package br.com.vivovaloriza.dbmetricsmonitor.intelligence.dto;

import br.com.vivovaloriza.dbmetricsmonitor.intelligence.enums.PenaltyCode;

public record ScorePenaltyResponse(
        PenaltyCode code,
        int points,
        String message
) {
}
