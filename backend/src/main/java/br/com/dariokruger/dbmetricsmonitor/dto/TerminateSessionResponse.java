package br.com.dariokruger.dbmetricsmonitor.dto;

public record TerminateSessionResponse(
        long pid,
        boolean terminated,
        String message
) {
}
