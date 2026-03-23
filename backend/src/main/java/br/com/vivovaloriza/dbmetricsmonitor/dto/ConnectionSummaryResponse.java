package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConnectionSummaryResponse(
        int totalConnections,
        int activeConnections,
        int idleConnections,
        int idleInTransactionConnections,
        int maxConnections,
        BigDecimal usagePercent,
        List<GroupedConnectionResponse> byUser,
        List<GroupedConnectionResponse> byApplication
) {
}
