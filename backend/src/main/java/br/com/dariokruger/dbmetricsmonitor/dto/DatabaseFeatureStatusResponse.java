package br.com.dariokruger.dbmetricsmonitor.dto;

public record DatabaseFeatureStatusResponse(
        boolean available,
        String message
) {
}
