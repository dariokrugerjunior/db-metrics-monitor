package br.com.dariokruger.dbmetricsmonitor.intelligence.dto;

import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.AlertCategory;
import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.AlertCode;
import br.com.dariokruger.dbmetricsmonitor.intelligence.enums.AlertSeverity;
import java.time.Instant;

public record AlertItemResponse(
        AlertCode code,
        String title,
        String description,
        AlertSeverity severity,
        AlertCategory category,
        Instant detectedAt,
        String suggestedAction
) {
}
