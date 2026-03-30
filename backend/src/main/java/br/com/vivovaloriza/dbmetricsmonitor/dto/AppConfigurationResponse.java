package br.com.vivovaloriza.dbmetricsmonitor.dto;

import java.time.Instant;

public record AppConfigurationResponse(
        String dbUrl,
        String dbUser,
        String dbPassword,
        String appOpenAiApiKey,
        int appOpenAiMaxOutputTokens,
        String activeDatasourceUrl,
        boolean databaseSettingsAppliedAtRuntime,
        boolean restartRequired,
        Instant savedAt
) {
}
