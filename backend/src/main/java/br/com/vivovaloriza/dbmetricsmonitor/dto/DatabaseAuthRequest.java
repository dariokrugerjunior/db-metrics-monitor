package br.com.vivovaloriza.dbmetricsmonitor.dto;

import jakarta.validation.constraints.NotBlank;

public record DatabaseAuthRequest(
        @NotBlank(message = "dbUrl e obrigatorio.")
        String dbUrl,

        @NotBlank(message = "dbUser e obrigatorio.")
        String dbUser,

        @NotBlank(message = "dbPassword e obrigatorio.")
        String dbPassword
) {
}
