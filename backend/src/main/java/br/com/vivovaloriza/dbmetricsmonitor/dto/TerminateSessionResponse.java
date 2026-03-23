package br.com.vivovaloriza.dbmetricsmonitor.dto;

public record TerminateSessionResponse(
        long pid,
        boolean terminated,
        String message
) {
}
