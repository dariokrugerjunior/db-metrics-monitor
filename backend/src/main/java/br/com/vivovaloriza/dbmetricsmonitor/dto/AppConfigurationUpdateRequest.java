package br.com.vivovaloriza.dbmetricsmonitor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AppConfigurationUpdateRequest(
        @NotBlank(message = "dbUrl e obrigatorio.")
        String dbUrl,
        @NotBlank(message = "dbUser e obrigatorio.")
        String dbUser,
        @NotBlank(message = "dbPassword e obrigatorio.")
        String dbPassword,
        @NotBlank(message = "APP_OPENAI_API_KEY e obrigatoria.")
        String appOpenAiApiKey,
        @Min(value = 1, message = "APP_OPENAI_MAX_OUTPUT_TOKENS deve ser maior que zero.")
        int appOpenAiMaxOutputTokens
) {
}
