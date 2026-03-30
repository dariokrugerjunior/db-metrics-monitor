package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AppConfigurationResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AppConfigurationUpdateRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseConnectionTestRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseConnectionTestResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RuntimeConfigurationService {

    private static final Path SETTINGS_FILE = Path.of("data", "app-runtime-settings.json");

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Value("${spring.datasource.url}")
    private String activeDatasourceUrl;

    @Value("${spring.datasource.username}")
    private String activeDatasourceUser;

    @Value("${spring.datasource.password}")
    private String activeDatasourcePassword;

    private volatile StoredConfiguration storedConfiguration;

    @PostConstruct
    void load() {
        if (!Files.exists(SETTINGS_FILE)) {
            return;
        }

        try {
            storedConfiguration = objectMapper.readValue(SETTINGS_FILE.toFile(), StoredConfiguration.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel carregar o arquivo de configuracao persistida.", ex);
        }
    }

    public AppConfigurationResponse getConfiguration() {
        EffectiveConfiguration effective = getEffectiveConfiguration();
        boolean appliedAtRuntime = isDatabaseSettingsAppliedAtRuntime(effective);

        return new AppConfigurationResponse(
                effective.dbUrl(),
                effective.dbUser(),
                effective.dbPassword(),
                effective.openAiApiKey(),
                effective.openAiMaxOutputTokens(),
                activeDatasourceUrl,
                appliedAtRuntime,
                !appliedAtRuntime,
                effective.savedAt()
        );
    }

    public AppConfigurationResponse updateConfiguration(AppConfigurationUpdateRequest request) {
        Instant savedAt = Instant.now();
        storedConfiguration = new StoredConfiguration(
                request.dbUrl().trim(),
                request.dbUser().trim(),
                request.dbPassword(),
                request.appOpenAiApiKey().trim(),
                request.appOpenAiMaxOutputTokens(),
                savedAt
        );
        persist(storedConfiguration);
        return getConfiguration();
    }

    public DatabaseConnectionTestResponse testConnection(DatabaseConnectionTestRequest request) {
        DriverManager.setLoginTimeout(5);
        long start = System.currentTimeMillis();

        try (Connection connection = DriverManager.getConnection(request.dbUrl(), request.dbUser(), request.dbPassword());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT current_database()")) {
            String currentDatabase = rs.next() ? rs.getString(1) : null;
            long elapsed = System.currentTimeMillis() - start;

            return new DatabaseConnectionTestResponse(
                    true,
                    connection.getMetaData().getDatabaseProductName(),
                    connection.getMetaData().getDatabaseProductVersion(),
                    currentDatabase,
                    elapsed,
                    "Conexao validada com sucesso."
            );
        } catch (SQLException ex) {
            long elapsed = System.currentTimeMillis() - start;
            return new DatabaseConnectionTestResponse(
                    false,
                    "PostgreSQL",
                    null,
                    null,
                    elapsed,
                    sanitizeSqlMessage(ex)
            );
        }
    }

    public String getEffectiveOpenAiApiKey() {
        EffectiveConfiguration effective = getEffectiveConfiguration();
        return effective.openAiApiKey();
    }

    public int getEffectiveOpenAiMaxOutputTokens() {
        EffectiveConfiguration effective = getEffectiveConfiguration();
        return effective.openAiMaxOutputTokens();
    }

    private EffectiveConfiguration getEffectiveConfiguration() {
        StoredConfiguration current = storedConfiguration;
        return new EffectiveConfiguration(
                current != null && hasText(current.dbUrl()) ? current.dbUrl() : activeDatasourceUrl,
                current != null && hasText(current.dbUser()) ? current.dbUser() : activeDatasourceUser,
                current != null && hasText(current.dbPassword()) ? current.dbPassword() : activeDatasourcePassword,
                current != null && hasText(current.appOpenAiApiKey()) ? current.appOpenAiApiKey() : appProperties.getAi().getApiKey(),
                current != null && current.appOpenAiMaxOutputTokens() > 0
                        ? current.appOpenAiMaxOutputTokens()
                        : appProperties.getAi().getMaxOutputTokens(),
                current == null ? null : current.savedAt()
        );
    }

    private boolean isDatabaseSettingsAppliedAtRuntime(EffectiveConfiguration effective) {
        return Objects.equals(effective.dbUrl(), activeDatasourceUrl)
                && Objects.equals(effective.dbUser(), activeDatasourceUser)
                && Objects.equals(effective.dbPassword(), activeDatasourcePassword);
    }

    private void persist(StoredConfiguration configuration) {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE.toFile(), configuration);
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel persistir a configuracao no backend.", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String sanitizeSqlMessage(SQLException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Falha ao conectar no banco informado.";
        }
        return ex.getMessage();
    }

    private record EffectiveConfiguration(
            String dbUrl,
            String dbUser,
            String dbPassword,
            String openAiApiKey,
            int openAiMaxOutputTokens,
            Instant savedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StoredConfiguration(
            String dbUrl,
            String dbUser,
            String dbPassword,
            String appOpenAiApiKey,
            int appOpenAiMaxOutputTokens,
            Instant savedAt
    ) {
    }
}
