package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.util.List;

public record QueryStatsResponse(
        boolean available,
        String message,
        List<TopQueryResponse> queries
) {
}
