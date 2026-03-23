package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.math.BigDecimal;

public record TopQueryResponse(
        String query,
        Long calls,
        BigDecimal totalExecTime,
        BigDecimal meanExecTime,
        Long rows,
        Long sharedBlksHit,
        Long sharedBlksRead,
        Long tempBlksWritten
) {
}
