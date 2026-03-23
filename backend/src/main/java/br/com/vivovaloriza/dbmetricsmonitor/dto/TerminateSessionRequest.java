package br.com.vivovaloriza.dbmetricsmonitor.dto;

import jakarta.validation.constraints.AssertTrue;

public record TerminateSessionRequest(
        String reason,
        @AssertTrue(message = "O campo force deve ser true para confirmar a finalizacao.")
        Boolean force
) {
}
