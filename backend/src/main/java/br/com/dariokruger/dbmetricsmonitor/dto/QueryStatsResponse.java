package br.com.dariokruger.dbmetricsmonitor.dto;

import java.util.List;

public record QueryStatsResponse(
        boolean available,
        String message,
        List<TopQueryResponse> queries
) {
}
