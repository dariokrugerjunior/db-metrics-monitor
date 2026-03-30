package br.com.dariokruger.dbmetricsmonitor.dto;

import jakarta.validation.constraints.NotBlank;

public record OpenAiConnectionTestRequest(
        @NotBlank(message = "APP_OPENAI_API_KEY e obrigatoria.")
        String apiKey
) {
}
