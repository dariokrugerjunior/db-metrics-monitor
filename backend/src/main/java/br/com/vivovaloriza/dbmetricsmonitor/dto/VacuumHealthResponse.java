package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VacuumHealthResponse(
        String schemaName,
        String tableName,
        long liveTuples,
        long deadTuples,
        BigDecimal deadTuplePercent,
        OffsetDateTime lastVacuum,
        OffsetDateTime lastAutovacuum,
        OffsetDateTime lastAnalyze,
        OffsetDateTime lastAutoanalyze
) {
}
