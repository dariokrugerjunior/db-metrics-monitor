package br.com.dariokruger.dbmetricsmonitor.model;

import br.com.dariokruger.dbmetricsmonitor.dto.DashboardSummaryResponse;
import java.time.Instant;

public record OperationalSnapshot(
        Instant generatedAt,
        DashboardSummaryResponse summary
) {
}
