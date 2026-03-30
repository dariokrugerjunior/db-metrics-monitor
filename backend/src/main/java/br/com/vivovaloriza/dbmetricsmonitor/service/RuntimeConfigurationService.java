package br.com.vivovaloriza.dbmetricsmonitor.service;

import br.com.vivovaloriza.dbmetricsmonitor.config.AppProperties;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AppConfigurationResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.AppConfigurationUpdateRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseAuthRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseAuthResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseConnectionTestRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.DatabaseConnectionTestResponse;
import br.com.vivovaloriza.dbmetricsmonitor.dto.OpenAiConnectionTestRequest;
import br.com.vivovaloriza.dbmetricsmonitor.dto.OpenAiConnectionTestResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
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
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

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

    public OpenAiConnectionTestResponse testOpenAiConnection(OpenAiConnectionTestRequest request) {
        long start = System.currentTimeMillis();

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getAi().getBaseUrl() + "/models"))
                    .header("Authorization", "Bearer " + request.apiKey().trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - start;

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new OpenAiConnectionTestResponse(
                        false,
                        0,
                        elapsed,
                        "Falha ao autenticar na OpenAI. Status HTTP " + response.statusCode() + "."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            int modelCount = root.path("data").isArray() ? root.path("data").size() : 0;

            return new OpenAiConnectionTestResponse(
                    true,
                    modelCount,
                    elapsed,
                    "Autenticacao OpenAI validada com sucesso."
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new OpenAiConnectionTestResponse(false, 0, System.currentTimeMillis() - start, "Erro ao testar a OpenAI.");
        } catch (IOException ex) {
            return new OpenAiConnectionTestResponse(false, 0, System.currentTimeMillis() - start, "Erro ao testar a OpenAI.");
        }
    }

    /**
     * Testa a conexao com o banco e, se bem-sucedida, persiste apenas as credenciais
     * de banco (preservando as configuracoes OpenAI existentes).
     * A senha nunca e incluida na resposta.
     */
    public DatabaseAuthResponse connectAndSaveDatabase(DatabaseAuthRequest request) {
        DatabaseConnectionTestRequest testReq = new DatabaseConnectionTestRequest(
                request.dbUrl(), request.dbUser(), request.dbPassword()
        );
        DatabaseConnectionTestResponse testResult = testConnection(testReq);

        if (!testResult.success()) {
            return new DatabaseAuthResponse(
                    false,
                    request.dbUrl(),
                    null,
                    null,
                    null,
                    testResult.responseTimeMs(),
                    testResult.message()
            );
        }

        saveDatabaseCredentials(request.dbUrl(), request.dbUser(), request.dbPassword());

        return new DatabaseAuthResponse(
                true,
                request.dbUrl(),
                testResult.databaseProductName(),
                testResult.databaseVersion(),
                testResult.currentDatabase(),
                testResult.responseTimeMs(),
                "Conexao realizada com sucesso."
        );
    }

    /**
     * Persiste somente as credenciais de banco, mantendo as configuracoes OpenAI atuais.
     */
    public void saveDatabaseCredentials(String dbUrl, String dbUser, String dbPassword) {
        EffectiveConfiguration current = getEffectiveConfiguration();
        Instant savedAt = Instant.now();
        storedConfiguration = new StoredConfiguration(
                dbUrl.trim(),
                dbUser.trim(),
                dbPassword,
                current.openAiApiKey(),
                current.openAiMaxOutputTokens(),
                savedAt
        );
        persist(storedConfiguration);
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
