package br.com.vivovaloriza.dbmetricsmonitor.model;

import br.com.vivovaloriza.dbmetricsmonitor.dto.DashboardSummaryResponse;
import java.time.Instant;

public record OperationalSnapshot(
        Instant generatedAt,
        DashboardSummaryResponse summary
) {
}
