package br.com.dariokruger.dbmetricsmonitor.intelligence.dto;

import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.PenaltyCode;

public record ScorePenaltyResponse(
        PenaltyCode code,
        int points,
        String message
) {
}
